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
package com.github.dirkbolte.wiremock.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(SAME_THREAD)
class StateRequestMatcherTest {

    private static final String TEST_URL = "/test";
    private static final StateRecordingAction stateRecordingAction = new StateRecordingAction();
    private static final ObjectMapper mapper = new ObjectMapper();
    @RegisterExtension
    public static WireMockExtension wm = WireMockExtension.newInstance()
        .options(
            wireMockConfig().dynamicPort().dynamicHttpsPort()
                .extensions(
                    stateRecordingAction,
                    new StateRequestMatcher(stateRecordingAction),
                    new ResponseTemplateTransformer(true)
                )
        )
        .build();

    @BeforeAll
    void setupAll() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeEach
    void setup() {
        wm.resetAll();
    }

    @Test
    void test_noExtensionUsage_ok() throws JsonProcessingException, URISyntaxException {
        var runtimeInfo = wm.getRuntimeInfo();
        wm.stubFor(
            post(urlEqualTo(TEST_URL))
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
            .post(new URI(runtimeInfo.getHttpBaseUrl() + TEST_URL))
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("testKey", equalTo("testValue"));
    }

    @Test
    void test_unknownContext_notFound() throws JsonProcessingException, URISyntaxException {
        createPostStub();
        createGetStub();

        String context = RandomStringUtils.randomAlphabetic(5);
        getAndAssertContextMatcher(context, HttpStatus.SC_NOT_FOUND, "context not found");
    }


    @Test
    void test_findsContext_ok() throws JsonProcessingException, URISyntaxException {
        var contextValue = RandomStringUtils.randomAlphabetic(5);

        createPostStub();
        createGetStub();

        var context = postAndAssertContextValue(contextValue);
        getAndAssertContextMatcher(context, HttpStatus.SC_OK, "context found");
    }


    @Test
    void test_unknownContextWithOtherContextAvailable_notFound() throws JsonProcessingException, URISyntaxException {
        var contextValue = RandomStringUtils.randomAlphabetic(5);

        createPostStub();
        createGetStub();

        var context = postAndAssertContextValue(contextValue);
        getAndAssertContextMatcher(context, HttpStatus.SC_OK, "context found");
        getAndAssertContextMatcher(RandomStringUtils.randomAlphabetic(5), HttpStatus.SC_NOT_FOUND, "context not found");
    }

    @Test
    void test_multipleContexts_ok() throws JsonProcessingException, URISyntaxException {
        var contextValueOne = RandomStringUtils.randomAlphabetic(5);
        var contextValueTwo = RandomStringUtils.randomAlphabetic(5);

        createPostStub();
        createGetStub();
        var contextOne = postAndAssertContextValue(contextValueOne);
        var contextTwo = postAndAssertContextValue(contextValueTwo);
        getAndAssertContextMatcher(contextOne, HttpStatus.SC_OK, "context found");
        getAndAssertContextMatcher(contextTwo, HttpStatus.SC_OK, "context found");
    }

    private void createGetStub() throws JsonProcessingException {
        wm.stubFor(
            get(urlPathMatching(TEST_URL + "/[^/]+"))
                .andMatching("state-matcher", Parameters.one("hasContext", "{{request.pathSegments.[1]}}"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            mapper.readTree(
                                mapper.writeValueAsString(Map.of("status", "context found")))
                        )
                )
        );
        wm.stubFor(
            get(urlPathMatching(TEST_URL + "/[^/]+"))
                .andMatching("state-matcher", Parameters.one("hasNotContext", "{{request.pathSegments.[1]}}"))
                .willReturn(
                    WireMock.notFound()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            mapper.readTree(
                                mapper.writeValueAsString(Map.of("status", "context not found")))
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
                .withPostServeAction(
                    "recordState",
                    Parameters.from(
                        Map.of(
                            "context", "{{jsonPath response.body '$.id'}}",
                            "state", Map.of(
                                "stateValue", "{{jsonPath request.body '$.contextValue'}}"
                            )
                        )
                    )
                )
        );
    }

    private void getAndAssertContextMatcher(String context, int httpStatus, String statusValue) throws URISyntaxException {
        var responseValue = given()
            .accept(ContentType.JSON)
            .get(new URI(String.format("%s%s/%s", wm.getRuntimeInfo().getHttpBaseUrl(), TEST_URL, context)))
            .then()
            .statusCode(httpStatus)
            .body("status", equalTo(statusValue));
    }

    private String postAndAssertContextValue(String contextValue) throws URISyntaxException {
        var context = given()
            .accept(ContentType.JSON)
            .body(Map.of("contextValue", contextValue))
            .post(new URI(wm.getRuntimeInfo().getHttpBaseUrl() + TEST_URL))
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
}