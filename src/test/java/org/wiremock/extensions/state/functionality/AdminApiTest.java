/*
 * Copyright (C) 2023 Dirk Bolte
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wiremock.extensions.state.functionality;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.extension.Parameters;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.RestAssured.given;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AdminApiTest extends AbstractTestBase {

    private static final String TEST_URL = "/test";

    private void createPostStub() {
        wm.stubFor(
            post(urlEqualTo(TEST_URL))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            Json.node(
                                Json.write(Map.of("id", "{{randomValue length=32 type='ALPHANUMERIC' uppercase=false}}"))
                            )
                        )
                )
                .withServeEventListener(
                    "recordState",
                    Parameters.from(
                        Map.of(
                            "context", "{{jsonPath response.body '$.id'}}",
                            "state", Map.of(
                                "stateValue", "{{jsonPath request.body '$.contextValue'}}"
                            ),
                            "list", Map.of(
                                "addLast", Map.of(
                                    "stateValue", "{{jsonPath request.body '$.contextValue'}}"
                                )
                            )
                        )
                    )
                )
        );

        wm.stubFor(
            post(urlPathMatching(TEST_URL + "/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withBody("{{jsonPath request.body '$'}}")
                )
                .withServeEventListener(
                    "recordState",
                    Parameters.from(
                        Map.of(
                            "context", "{{jsonPath response.body '$.id'}}",
                            "state", Map.of(
                                "stateValue", "{{jsonPath request.body '$.contextValue' default='null'}}"
                            ),
                            "list", Map.of(
                                "addLast", Map.of(
                                    "stateValue", "{{jsonPath request.body '$.contextValue'}}"
                                )
                            )
                        )
                    )
                )
        );
    }

    private String postAndAssertContextValue(String contextValue) {
        var context = given()
            .accept(ContentType.JSON)
            .body(Map.of("contextValue", contextValue))
            .post(assertDoesNotThrow(() -> new URI(wm.getRuntimeInfo().getHttpBaseUrl() + TEST_URL)))
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", Matchers.notNullValue())
            .extract()
            .body()
            .jsonPath().getString("id");

        assertThat(context)
            .isNotNull()
            .isNotEmpty();

        return context;
    }

    private String postAndAssertContextValue(String contextName, String contextValue) {
        var context = given()
            .accept(ContentType.JSON)
            .body(new HashMap<String, String>() {{
                put("contextValue", contextValue);
                put("id", contextName);
            }})
            .post(assertDoesNotThrow(() -> new URI(wm.getRuntimeInfo().getHttpBaseUrl() + TEST_URL + "/" + contextName)))
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", Matchers.notNullValue())
            .extract()
            .body()
            .jsonPath().getString("id");

        assertThat(context)
            .isNotNull()
            .isNotEmpty();

        return context;
    }

    private List<String> getContexts() {
        return given()
            .accept(ContentType.JSON)
            .get(assertDoesNotThrow(() -> new URI(wm.getRuntimeInfo().getHttpBaseUrl() + "/__admin/state-extension/contexts")))
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
    }

    private Map<String, Object> getContext(String contextName) {
        return getContext(contextName, HttpStatus.SC_OK)
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
    }

    private ValidatableResponse getContext(String contextName, int status) {
        return given()
            .accept(ContentType.JSON)
            .get(assertDoesNotThrow(() -> new URI(wm.getRuntimeInfo().getHttpBaseUrl() + "/__admin/state-extension/contexts/" + contextName)))
            .then()
            .statusCode(status);
    }

    private ValidatableResponse deleteContext(String contextName) {
        return given()
            .accept(ContentType.JSON)
            .delete(assertDoesNotThrow(() -> new URI(wm.getRuntimeInfo().getHttpBaseUrl() + "/__admin/state-extension/contexts/" + contextName)))
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);
    }


    private ValidatableResponse deleteAllContexts() {
        return given()
            .accept(ContentType.JSON)
            .delete(assertDoesNotThrow(() -> new URI(wm.getRuntimeInfo().getHttpBaseUrl() + "/__admin/state-extension/contexts")))
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @BeforeEach
    void setup() {
        createPostStub();
    }


    @DisplayName("get all")
    @Nested
    public class GetAllContexts {

        @DisplayName("no contexts")
        @Test
        void test_no_contexts() {
            assertThat(getContexts()).isEmpty();
        }

        @DisplayName("multiple contexts")
        @Test
        void test_multiple_contexts() {
            postAndAssertContextValue("context1", randomAlphabetic(5));
            postAndAssertContextValue("context2", randomAlphabetic(5));
            assertThat(getContexts()).hasSize(2).containsExactlyInAnyOrder("context1", "context2");
        }
    }

    @DisplayName("Get single")
    @Nested
    public class GetContext {

        @DisplayName("no context")
        @Test
        void test_no_context() {
            getContext("context1", HttpStatus.SC_NOT_FOUND);
        }

        @DisplayName("multiple contexts")
        @Test
        void test_multiple_contexts() {
            postAndAssertContextValue("context1", "context1Value");
            postAndAssertContextValue("context2", "context2Value");

            assertThat(getContext("context1"))
                .containsEntry("contextName", "context1")
                .hasEntrySatisfying("properties", (context) -> {
                        assertThat(context).isInstanceOfSatisfying(Map.class, (properties) -> {
                            assertThat(properties).hasSize(1);
                            assertThat(properties).containsEntry("stateValue", "context1Value");
                        });
                    }
                );
            assertThat(getContext("context2"))
                .containsEntry("contextName", "context2")
                .hasEntrySatisfying("properties", (context) -> {
                        assertThat(context).isInstanceOfSatisfying(Map.class, (properties) -> {
                            assertThat(properties).hasSize(1);
                            assertThat(properties).containsEntry("stateValue", "context2Value");
                        });
                    }
                );
        }
    }

    @DisplayName("Delete single")
    @Nested
    public class DeleteContext {

        @DisplayName("non-existant context")
        @Test
        void test_non_existant_context() {
            deleteContext("context1");

            assertThat(getContexts()).isEmpty();
        }

        @DisplayName("single context")
        @Test
        void test_single_context() {
            postAndAssertContextValue("context1", randomAlphabetic(5));
            assertThat(getContexts()).hasSize(1);

            deleteContext("context1");

            assertThat(getContexts()).isEmpty();
        }


        @DisplayName("one of multiple contexts")
        @Test
        void test_one_of_multiple_contexts() {
            postAndAssertContextValue("context1", randomAlphabetic(5));
            postAndAssertContextValue("context2", randomAlphabetic(5));
            postAndAssertContextValue("context3", randomAlphabetic(5));

            deleteContext("context2");

            assertThat(getContexts()).containsExactlyInAnyOrder("context1", "context3");
        }

    }

    @DisplayName("Delete all")
    @Nested
    public class DeleteAllContexts {

        @DisplayName("non-existant context")
        @Test
        void test_non_existant_context() {
            deleteAllContexts();

            assertThat(getContexts()).isEmpty();
        }

        @DisplayName("multiple contexts")
        @Test
        void test_multiple_contexts() {
            postAndAssertContextValue("context1", randomAlphabetic(5));
            postAndAssertContextValue("context2", randomAlphabetic(5));
            postAndAssertContextValue("context3", randomAlphabetic(5));

            deleteAllContexts();

            assertThat(getContexts()).isEmpty();
        }

    }
}