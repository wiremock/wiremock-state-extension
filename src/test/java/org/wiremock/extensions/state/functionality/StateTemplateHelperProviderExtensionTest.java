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
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.extension.Parameters;
import io.restassured.http.ContentType;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class StateTemplateHelperProviderExtensionTest extends AbstractTestBase {

    @Test
    void test_noExtensionUsage_ok() throws JsonProcessingException, URISyntaxException {
        var runtimeInfo = wm.getRuntimeInfo();
        wm.stubFor(
            post(urlEqualTo("/"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            mapper.readTree(
                                mapper.writeValueAsString(Map.of("testKey", "testValue")))
                        )
                )
        );

        given()
            .accept(ContentType.JSON)
            .post(new URI(runtimeInfo.getHttpBaseUrl() + "/"))
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("testKey", equalTo("testValue"));
    }

    @Test
    void test_unknownContext_fail() {
        createPostStub();
        createGetStub();

        String context = RandomStringUtils.randomAlphabetic(5);
        getAndAssertContextValue(
            "state",
            context,
            String.format("[ERROR: No state for context %s, property stateValueOne found]", context),
            String.format("[ERROR: No state for context %s, property stateValueTwo found]", context),
            String.format("[ERROR: No state for context %s, property listSize found]", context)
        );
    }

    @Test
    void test_unknownContext_useDefault() {
        createPostStub();
        createGetStub();

        String context = RandomStringUtils.randomAlphabetic(5);
        getAndAssertContextValue(
            "state/default",
            context,
            "defaultStateValueOne",
            "defaultStateValueTwo",
            "defaultListSize"
        );
    }

    @Test
    void test_unknownProperty_fail() throws JsonProcessingException {
        var contextValue = RandomStringUtils.randomAlphabetic(5);

        createPostStub();
        wm.stubFor(
            get(urlPathMatching("/state/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            mapper.readTree(
                                mapper.writeValueAsString(Map.of(
                                    "valueOne", "{{state context=request.pathSegments.[1] property='unknownValue'}}",
                                    "valueTwo", "{{state context=request.pathSegments.[1] property='unknownValue'}}",
                                    "unknown", "{{state context=request.pathSegments.[1] property='unknown' default='defaultUnknown'}}"
                                )))
                        )
                )
        );

        postAndAssertContextValue("state", contextValue, "one");
        getAndAssertContextValue(
            "state",
            contextValue,
            String.format("[ERROR: No state for context %s, property unknownValue found]", contextValue),
            String.format("[ERROR: No state for context %s, property unknownValue found]", contextValue),
            null
        );
    }

    @Nested
    public class Property {

        @BeforeEach
        void setup() {
            createPostStub();
            createGetStub();
        }

        @Test
        void test_returnsStateFromPreviousRequest_ok() {
            var contextValue = RandomStringUtils.randomAlphabetic(5);

            postAndAssertContextValue("state", contextValue, "one");
            getAndAssertContextValue("state", contextValue, contextValue, "one", "0");
        }

        @Test
        void test_defaults_returnsStateFromPreviousRequest_ok() {
            var contextValue = RandomStringUtils.randomAlphabetic(5);

            postAndAssertContextValue("state", contextValue, "one");
            getAndAssertContextValue("state/default", contextValue, contextValue, "one", "0");
        }

        @Test
        void test_returnsFullBodyFromPreviousRequest_ok() {
            var contextValue = RandomStringUtils.randomAlphabetic(5);

            postAndAssertContextValue("state", contextValue, "one");
            getAndAssertFullBody(contextValue);
        }

        @Test
        void test_differentStatesSupported_ok() {
            var contextValueOne = RandomStringUtils.randomAlphabetic(5);
            var contextValueTwo = RandomStringUtils.randomAlphabetic(5);

            postAndAssertContextValue("state", contextValueOne, "one");
            postAndAssertContextValue("state", contextValueTwo, "one");
            getAndAssertContextValue("state", contextValueOne, contextValueOne, "one", "0");
            getAndAssertContextValue("state", contextValueTwo, contextValueTwo, "one", "0");
        }


    }
    @Nested
    public class List {
        @BeforeEach
        void setup() {
            createPostStub();
            createGetStub();
        }

        @Test
        void test_returnsListElement_oneItem_ok() {
            var contextValue = RandomStringUtils.randomAlphabetic(5);

            postAndAssertContextValue("list", contextValue, "one");

            getAndAssertContextValue("list/0", contextValue, contextValue, "one", "1");
        }

        @Test
        void test_defaults_knownItem_ok() {
            var contextValue = RandomStringUtils.randomAlphabetic(5);

            postAndAssertContextValue("list", contextValue, "one");

            getAndAssertContextValue("list/default/0", contextValue, contextValue, "one", "1");
        }

        @Test
        void test_defaults_unknownItem_ok() {
            var contextValue = RandomStringUtils.randomAlphabetic(5);

            postAndAssertContextValue("list", contextValue, "one");

            getAndAssertContextValue("list/default/1", contextValue, "defaultStateValueOne", "defaultStateValueTwo", "1");
        }

        @Test
        void test_returnsListElement_multipleItems_ok() {
            var contextValue = RandomStringUtils.randomAlphabetic(5);

            postAndAssertContextValue("list", contextValue, "one");
            postAndAssertContextValue("list", contextValue, "two");
            postAndAssertContextValue("list", contextValue, "three");

            getAndAssertContextValue("list/1", contextValue, contextValue, "two", "3");
        }
        @Test
        void test_returnsSingleListElement_lastItem_ok() {
            var contextValue = RandomStringUtils.randomAlphabetic(5);

            postAndAssertContextValue("list", contextValue, "one");
            postAndAssertContextValue("list", contextValue, "two");
            postAndAssertContextValue("list", contextValue, "three");

            getAndAssertContextValue("list/-1", contextValue, contextValue, "three", "3");
        }

    }
    private void createGetStub() {
        wm.stubFor(
            get(urlPathMatching("/state/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            Json.node(
                                Json.write(
                                    Map.of(
                                        "valueOne", "{{state context=request.pathSegments.[1] property='stateValueOne'}}",
                                        "valueTwo", "{{state context=request.pathSegments.[1] property='stateValueTwo'}}",
                                        "listSize", "{{state context=request.pathSegments.[1] property='listSize'}}",
                                        "unknown", "{{state context=request.pathSegments.[1] property='unknown' default='defaultUnknown'}}"
                                    )
                                )
                            )
                        )
                )
        );
        wm.stubFor(
            get(urlPathMatching("/state/default/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            Json.node(
                                Json.write(
                                    Map.of(
                                        "valueOne", "{{state context=request.pathSegments.[2] property='stateValueOne' default='defaultStateValueOne'}}",
                                        "valueTwo", "{{state context=request.pathSegments.[2] property='stateValueTwo'  default='defaultStateValueTwo'}}",
                                        "listSize", "{{state context=request.pathSegments.[2] property='listSize' default='defaultListSize'}}",
                                        "unknown", "{{state context=request.pathSegments.[2] property='unknown' default='defaultUnknown'}}"
                                    )
                                )
                            )
                        )
                )
        );
        wm.stubFor(
            get(urlPathMatching("/full/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withBody("{{{state context=request.pathSegments.[1] property='stateBody'}}}")
                )
        );
        wm.stubFor(
            get(urlPathMatching("/list/[^/]+/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            Json.node(
                                Json.write(
                                    Map.of(
                                        "valueOne", "{{state context=request.pathSegments.[2] list=(join '[' request.pathSegments.[1] '].stateValueOne' '')}}",
                                        "valueTwo", "{{state context=request.pathSegments.[2] list=(join '[' request.pathSegments.[1] '].stateValueTwo' '')}}",
                                        "listSize", "{{state context=request.pathSegments.[2] property='listSize'}}",
                                        "unknown", "{{state context=request.pathSegments.[1] property='unknown' default='defaultUnknown'}}"
                                    )
                                )
                            )
                        )
                )
        );

        wm.stubFor(
            get(urlPathMatching("/list/default/[^/]+/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            Json.node(
                                Json.write(
                                    Map.of(
                                        "valueOne", "{{state context=request.pathSegments.[3] list=(join '[' request.pathSegments.[2] '].stateValueOne' '') default='defaultStateValueOne'}}",
                                        "valueTwo", "{{state context=request.pathSegments.[3] list=(join '[' request.pathSegments.[2] '].stateValueTwo' '') default='defaultStateValueTwo'}}",
                                        "listSize", "{{state context=request.pathSegments.[3] property='listSize'  default='defaultListSize'}}",
                                        "unknown", "{{state context=request.pathSegments.[3] property='unknown' default='defaultUnknown'}}"
                                    )
                                )
                            )
                        )
                )
        );
    }

    private void createPostStub() {
        wm.stubFor(
            post(urlEqualTo("/state"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(Json.node(Json.write(Map.of(
                                        "id", "{{randomValue length=32 type='ALPHANUMERIC' uppercase=false}}",
                                        "contextValue", "{{jsonPath request.body '$.contextValueOne'}}",
                                        "other", "randomValue length=32 type='ALPHANUMERIC'"
                                    )
                                )
                            )
                        )
                )
                .withServeEventListener(
                    "recordState",
                    Parameters.from(
                        Map.of(
                            "context", "{{jsonPath request.body '$.contextValueOne'}}",
                            "state", Map.of(
                                "stateValueOne", "{{jsonPath request.body '$.contextValueOne'}}",
                                "stateValueTwo", "{{jsonPath request.body '$.contextValueTwo'}}",
                                "stateBody", "{{{jsonPath response.body '$'}}}"
                            )
                        )
                    )
                )
        );
        wm.stubFor(
            post(urlEqualTo("/list"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(Json.node(
                                Json.write(
                                    Map.of(
                                        "id", "{{randomValue length=32 type='ALPHANUMERIC' uppercase=false}}",
                                        "contextValue", "{{jsonPath request.body '$.contextValueOne'}}",
                                        "other", "randomValue length=32 type='ALPHANUMERIC'"
                                    )
                                )
                            )
                        )
                )
                .withServeEventListener(
                    "recordState",
                    Parameters.from(
                        Map.of(
                            "context", "{{jsonPath request.body '$.contextValueOne'}}",
                            "list", Map.of(
                                "addLast", Map.of(
                                    "stateValueOne", "{{jsonPath request.body '$.contextValueOne'}}",
                                    "stateValueTwo", "{{jsonPath request.body '$.contextValueTwo'}}",
                                    "stateBody", "{{{jsonPath response.body '$'}}}"
                                )
                            )
                        )
                    )
                )
        );
    }

    private void getAndAssertContextValue(String path, String context, String valueOne, String valueTwo, String listSize) {
        given()
            .accept(ContentType.JSON)
            .get(assertDoesNotThrow(() -> new URI(String.format("%s/%s/%s", wm.getRuntimeInfo().getHttpBaseUrl(), path, context))))
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("valueOne", equalTo(valueOne))
            .body("valueTwo", equalTo(valueTwo))
            .body("listSize", equalTo(listSize))
            .body("unknown", equalTo("defaultUnknown"))
            .body("other", nullValue());
    }

    private void getAndAssertFullBody(String contextValue) {
        given()
            .accept(ContentType.JSON)
            .get(assertDoesNotThrow(() -> new URI(String.format("%s/%s/%s", wm.getRuntimeInfo().getHttpBaseUrl(), "full", contextValue))))
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("contextValue", equalTo(contextValue))
            .body("other", notNullValue())
            .body("value", nullValue());
    }

    private void postAndAssertContextValue(String path, String contextValueOne, String contextValueTwo) {
        given()
            .accept(ContentType.JSON)
            .body(Map.of(
                    "contextValueOne", contextValueOne,
                    "contextValueTwo", contextValueTwo
                )
            )
            .post(assertDoesNotThrow(() -> new URI(wm.getRuntimeInfo().getHttpBaseUrl() + "/" + path)))
            .then()
            .statusCode(HttpStatus.SC_OK);
    }
}