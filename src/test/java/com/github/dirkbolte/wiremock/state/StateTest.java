package com.github.dirkbolte.wiremock.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
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
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(SAME_THREAD)
class StateTest {

    private static final String TEST_URL = "/test";
    private static final StateRecordingAction stateRecordingAction = new StateRecordingAction();
    private static final ObjectMapper mapper = new ObjectMapper();
    @RegisterExtension
    public static WireMockExtension wm = WireMockExtension.newInstance()
        .options(
            wireMockConfig().dynamicPort().dynamicHttpsPort()
                .extensions(
                    stateRecordingAction,
                    new ResponseTemplateTransformer(true, "state", new StateHelper(stateRecordingAction))
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
            .body("testKey", Matchers.equalTo("testValue"));
    }

    @Test
    void test_unknownContext_fail() throws JsonProcessingException, URISyntaxException {
        createPostStub();
        createGetStub();

        String context = RandomStringUtils.randomAlphabetic(5);
        getAndAssertContextValue(context, String.format("[ERROR: No state for context %s, property stateValue found]", context));
    }

    @Test
    void test_unknownProperty_fail() throws JsonProcessingException, URISyntaxException {
        var contextValue = RandomStringUtils.randomAlphabetic(5);

        createPostStub();
        wm.stubFor(
            get(urlPathMatching(TEST_URL + "/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            mapper.readTree(
                                mapper.writeValueAsString(Map.of("value", "{{state context=request.pathSegments.[1] property='unknownValue'}}")))
                        )
                )
        );

        var context = postAndAssertContextValue(contextValue);
        getAndAssertContextValue(context, String.format("[ERROR: No state for context %s, property unknownValue found]", context));
    }

    @Test
    void test_returnsStateFromPreviousRequest_ok() throws JsonProcessingException, URISyntaxException {
        var contextValue = RandomStringUtils.randomAlphabetic(5);

        createPostStub();
        createGetStub();

        var context = postAndAssertContextValue(contextValue);
        getAndAssertContextValue(context, contextValue);
    }

    @Test
    void test_differentStatesSupported_ok() throws JsonProcessingException, URISyntaxException {
        var contextValueOne = RandomStringUtils.randomAlphabetic(5);
        var contextValueTwo = RandomStringUtils.randomAlphabetic(5);

        createPostStub();
        createGetStub();

        var contextOne = postAndAssertContextValue(contextValueOne);
        var contextTwo = postAndAssertContextValue(contextValueTwo);
        getAndAssertContextValue(contextOne, contextValueOne);
        getAndAssertContextValue(contextTwo, contextValueTwo);
    }

    private void createGetStub() throws JsonProcessingException {
        wm.stubFor(
            get(urlPathMatching(TEST_URL + "/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            mapper.readTree(
                                mapper.writeValueAsString(Map.of("value", "{{state context=request.pathSegments.[1] property='stateValue'}}")))
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

    private void getAndAssertContextValue(String context, String contextValue) throws URISyntaxException {
        var responseValue = given()
            .accept(ContentType.JSON)
            .get(new URI(String.format("%s%s/%s", wm.getRuntimeInfo().getHttpBaseUrl(), TEST_URL, context)))
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("value", Matchers.notNullValue())
            .extract()
            .body()
            .jsonPath().get("value");

        assertThat(responseValue).describedAs("context value is returned").isEqualTo(contextValue);
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