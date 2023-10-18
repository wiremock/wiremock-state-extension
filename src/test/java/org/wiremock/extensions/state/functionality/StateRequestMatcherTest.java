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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.Parameters;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class StateRequestMatcherTest extends FunctionalTestBase {

    private static final String TEST_URL = "/test";

    @BeforeEach
    void setup() throws JsonProcessingException {
        createPostStub();
        createGetStubs();
        createDeleteStub();
    }

    private void createDeleteStub() {
        wm.stubFor(
            delete(urlPathMatching(TEST_URL + "/all/[^/]+"))
                .andMatching("state-matcher", Parameters.one("hasContext", "{{request.pathSegments.[2]}}"))
                .willReturn(WireMock.ok())
                .withServeEventListener(
                    "deleteState",
                    Parameters.from(
                        Map.of(
                            "context", "{{jsonPath response.body '$.id'}}",
                            "list", Map.of("deleteFirst", true)
                        )
                    )
                )
        );
    }

    private void createGetStubs() throws JsonProcessingException {
        wm.stubFor(
            get(urlPathMatching(TEST_URL + "/all/[^/]+"))
                .andMatching("state-matcher", Parameters.one("hasContext", "{{request.pathSegments.[2]}}"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            mapper.readTree(
                                mapper.writeValueAsString(Map.of(
                                    "status", "context found",
                                    "updateCount", "{{state context=request.pathSegments.[2] property='updateCount'}}",
                                    "listSize", "{{state context=request.pathSegments.[2] property='listSize'}}"
                                )))
                        )
                )
        );
        wm.stubFor(
            get(urlPathMatching(TEST_URL + "/all/[^/]+"))
                .andMatching("state-matcher", Parameters.one("hasNotContext", "{{request.pathSegments.[2]}}"))
                .willReturn(
                    WireMock.notFound()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            mapper.readTree(
                                mapper.writeValueAsString(Map.of("status", "context not found")))
                        )
                )
        );
        createGetStubs("hasProperty");
        createGetStubs("hasNotProperty");
        createGetStubs("updateCountEqualTo");
        createGetStubs("updateCountLessThan");
        createGetStubs("updateCountMoreThan");
        createGetStubs("listSizeEqualTo");
        createGetStubs("listSizeLessThan");
        createGetStubs("listSizeMoreThan");
    }

    private void createGetStubs(String check) throws JsonProcessingException {
        wm.stubFor(
            get(urlPathMatching(TEST_URL + "/" + check + "/[^/]+/[^/]+"))
                .andMatching("state-matcher", Parameters.from(
                    Map.of(
                        "hasContext", "{{request.pathSegments.[3]}}",
                        check, "{{request.pathSegments.[2]}}"
                    ))
                )
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            mapper.readTree(
                                mapper.writeValueAsString(Map.of(
                                    "status", "context found",
                                    "updateCount", "{{state context=request.pathSegments.[3] property='updateCount'}}",
                                    "listSize", "{{state context=request.pathSegments.[3] property='listSize'}}"
                                )))
                        )
                )
        );
    }

    private void createPostStub() throws JsonProcessingException {
        wm.stubFor(
            post(urlEqualTo(TEST_URL))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            mapper.readTree(
                                mapper.writeValueAsString(Map.of("id", "{{randomValue length=32 type='ALPHANUMERIC' uppercase=false}}")))
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

    private ValidatableResponse getAndAssertContextMatcher(String context, String path, int httpStatus, String statusValue) {
        return getAndAssertContextMatcher(context, path, httpStatus)
            .body("status", equalTo(statusValue));
    }

    private ValidatableResponse getAndAssertContextMatcher(String context, String path, int httpStatus) {
        return given()
            .accept(ContentType.JSON)
            .accept(ContentType.TEXT)
            .get(assertDoesNotThrow(() -> new URI(String.format("%s%s/%s/%s", wm.getRuntimeInfo().getHttpBaseUrl(), TEST_URL, path, context))))
            .then()
            .statusCode(httpStatus);
    }

    private void getAndAssertContextMatcher(String context, String path, int httpStatus, String statusValue, String updateCount, String listSize) {
        getAndAssertContextMatcher(context, path, httpStatus, statusValue)
            .body("updateCount", equalTo(updateCount))
            .body("listSize", equalTo(listSize));
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

    @Nested
    public class HasNotContext {
        @Test
        void test_unknownContext_notFound() {
            String context = RandomStringUtils.randomAlphabetic(5);
            getAndAssertContextMatcher(context, "all", HttpStatus.SC_NOT_FOUND, "context not found");
        }
    }

    @Nested
    public class hasContext {

        @Test
        void test_findsContext_ok() {
            var contextValue = RandomStringUtils.randomAlphabetic(5);

            var context = postAndAssertContextValue(contextValue);
            getAndAssertContextMatcher(context, "all", HttpStatus.SC_OK, "context found", "2", "1");
        }

        @Test
        void test_unknownContextWithOtherContextAvailable_notFound() {
            var contextValue = RandomStringUtils.randomAlphabetic(5);

            var context = postAndAssertContextValue(contextValue);
            getAndAssertContextMatcher(context, "all", HttpStatus.SC_OK, "context found", "2", "1");
            getAndAssertContextMatcher(RandomStringUtils.randomAlphabetic(5), "all", HttpStatus.SC_NOT_FOUND, "context not found");
        }

        @Test
        void test_multipleContexts_ok() {
            var contextValueOne = RandomStringUtils.randomAlphabetic(5);
            var contextValueTwo = RandomStringUtils.randomAlphabetic(5);

            var contextOne = postAndAssertContextValue(contextValueOne);
            var contextTwo = postAndAssertContextValue(contextValueTwo);

            getAndAssertContextMatcher(contextOne, "all", HttpStatus.SC_OK, "context found", "2", "1");
            getAndAssertContextMatcher(contextTwo, "all", HttpStatus.SC_OK, "context found", "2", "1");
        }

        @Nested
        public class Property {
            @Test
            void test_hasProperty_propertyDoesNotExist_fail() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                getAndAssertContextMatcher(context, "hasProperty/unknownValue", HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void test_hasProperty_propertyExists_ok() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                getAndAssertContextMatcher(context, "hasProperty/stateValue", HttpStatus.SC_OK, "context found", "2", "1");
            }

            @Test
            void test_hasProperty_propertyGotDeleted_fail() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                postAndAssertContextValue(contextValue);
                postAndAssertContextValue(context, null);
                getAndAssertContextMatcher(context, "hasProperty/stateValue", HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void test_hasNotProperty_propertyExists_fail() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                getAndAssertContextMatcher(context, "hasNotProperty/stateValue", HttpStatus.SC_NOT_FOUND);

            }

            @Test
            void test_hasNotProperty_propertyDoesNotExists_ok() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                getAndAssertContextMatcher(context, "hasNotProperty/unknownValue", HttpStatus.SC_OK, "context found", "2", "1");

            }
        }

        @Nested
        public class UpdateCount {
            @Test
            void test_countAndSizeIncreased_ok() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                getAndAssertContextMatcher(context, "all", HttpStatus.SC_OK, "context found", "2", "1");
                postAndAssertContextValue(context, contextValue);
                getAndAssertContextMatcher(context, "all", HttpStatus.SC_OK, "context found", "4", "2");
            }

            @Test
            void test_updateCountEqualTo_ok() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                postAndAssertContextValue(context, contextValue);
                postAndAssertContextValue(context, contextValue);
                getAndAssertContextMatcher(context, "updateCountEqualTo/6", HttpStatus.SC_OK, "context found", "6", "3");
                getAndAssertContextMatcher(context, "updateCountEqualTo/2", HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void test_listSizeEqualTo_ok() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                postAndAssertContextValue(context, contextValue);
                postAndAssertContextValue(context, contextValue);
                getAndAssertContextMatcher(context, "listSizeEqualTo/3", HttpStatus.SC_OK, "context found", "6", "3");
                getAndAssertContextMatcher(context, "listSizeEqualTo/2", HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void test_listSizeEqualTo_invalidInput_fail() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                getAndAssertContextMatcher(context, "listSizeEqualTo/invalid", HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void test_updateCountLessThan_ok() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                postAndAssertContextValue(context, contextValue);
                postAndAssertContextValue(context, contextValue);
                getAndAssertContextMatcher(context, "updateCountLessThan/7", HttpStatus.SC_OK, "context found", "6", "3");
                getAndAssertContextMatcher(context, "updateCountLessThan/6", HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void test_updateCountLessThan_invalidInput_fail() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                getAndAssertContextMatcher(context, "updateCountLessThan/invalid", HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void test_listSizeLessThan_ok() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                postAndAssertContextValue(context, contextValue);
                postAndAssertContextValue(context, contextValue);
                getAndAssertContextMatcher(context, "listSizeLessThan/4", HttpStatus.SC_OK, "context found", "6", "3");
                getAndAssertContextMatcher(context, "listSizeLessThan/3", HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void test_listSizeLessThan_invalidInput_fail() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                getAndAssertContextMatcher(context, "listSizeLessThan/invalid", HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void test_updateCountMoreThan_ok() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                postAndAssertContextValue(context, contextValue);
                postAndAssertContextValue(context, contextValue);
                getAndAssertContextMatcher(context, "updateCountMoreThan/5", HttpStatus.SC_OK, "context found", "6", "3");
                getAndAssertContextMatcher(context, "updateCountMoreThan/6", HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void test_updateCountMoreThan_invalidInput_fail() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                getAndAssertContextMatcher(context, "updateCountMoreThan/invalid", HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void test_listSizeMoreThan_ok() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                postAndAssertContextValue(context, contextValue);
                postAndAssertContextValue(context, contextValue);
                getAndAssertContextMatcher(context, "listSizeMoreThan/2", HttpStatus.SC_OK, "context found", "6", "3");
                getAndAssertContextMatcher(context, "listSizeMoreThan/3", HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void test_listSizeMoreThan_invalidInput_fail() {
                var contextValue = RandomStringUtils.randomAlphabetic(5);

                var context = postAndAssertContextValue(contextValue);
                getAndAssertContextMatcher(context, "listSizeMoreThan/invalid", HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void test_multipleContexts_updateAndSizeIncreasedIndividually_ok() {
                var contextValueOne = RandomStringUtils.randomAlphabetic(5);
                var contextValueTwo = RandomStringUtils.randomAlphabetic(5);

                var contextOne = postAndAssertContextValue(contextValueOne);
                var contextTwo = postAndAssertContextValue(contextValueTwo);

                getAndAssertContextMatcher(contextOne, "all", HttpStatus.SC_OK, "context found", "2", "1");
                getAndAssertContextMatcher(contextTwo, "all", HttpStatus.SC_OK, "context found", "2", "1");

                postAndAssertContextValue(contextOne, contextValueOne);
                getAndAssertContextMatcher(contextOne, "all", HttpStatus.SC_OK, "context found", "4", "2");
                getAndAssertContextMatcher(contextTwo, "all", HttpStatus.SC_OK, "context found", "2", "1");

                postAndAssertContextValue(contextTwo, contextValueTwo);
                getAndAssertContextMatcher(contextOne, "all", HttpStatus.SC_OK, "context found", "4", "2");
                getAndAssertContextMatcher(contextTwo, "all", HttpStatus.SC_OK, "context found", "4", "2");
            }
        }

    }
}