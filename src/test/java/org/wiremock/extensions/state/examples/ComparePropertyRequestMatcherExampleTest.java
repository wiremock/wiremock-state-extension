package org.wiremock.extensions.state.examples;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.store.Store;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.wiremock.extensions.state.CaffeineStore;
import org.wiremock.extensions.state.StateExtension;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

/**
 * Sample test for using this compare numeric property request matching.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(SAME_THREAD)
class ComparePropertyRequestMatcherExampleTest {

    private static final String TEST_URLENC_URL = "/test-urlenc";
    private static final String TEST_JSON_URL = "/test-json";
    private static final Store<String, Object> store = new CaffeineStore();

    @RegisterExtension
    public static WireMockExtension wm = WireMockExtension.newInstance()
        .options(
            wireMockConfig().dynamicPort().dynamicHttpsPort().templatingEnabled(true).globalTemplating(true)
                .extensions(new StateExtension(store))
                .notifier(new ConsoleNotifier(true))
        )
        .build();


    @BeforeEach
    public void setup() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        firstStubUrlenc();
        firstStubJson();
        secondStubUrlenc();
        secondStubJson();
    }

    @Test
    public void testUrlenc() {
        var id = given()
            .accept(ContentType.URLENC)
            .body("clientId=" + UUID.randomUUID() + "&amount=1000")
            .post(assertDoesNotThrow(() -> new URI(wm.getRuntimeInfo().getHttpBaseUrl() + TEST_URLENC_URL)))
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", Matchers.notNullValue())
            .extract()
            .body()
            .jsonPath().get("id");

        given()
            .accept(ContentType.URLENC)
            .body("id=" + id + "&amount=1000")
            .post(assertDoesNotThrow(() -> new URI(wm.getRuntimeInfo().getHttpBaseUrl() + TEST_URLENC_URL)))
            .then()
            .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testJson() {
        var id = given()
            .accept(ContentType.JSON)
            .body("{\"clientId\":\"" + UUID.randomUUID() + "\"}")
            .post(assertDoesNotThrow(() -> new URI(wm.getRuntimeInfo().getHttpBaseUrl() + TEST_JSON_URL)))
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", Matchers.notNullValue())
            .extract()
            .body()
            .jsonPath().get("id");

        given()
            .accept(ContentType.URLENC)
            .body("{\"id\":\"" + id + "\", \"amount\": 1000}")
            .post(assertDoesNotThrow(() -> new URI(wm.getRuntimeInfo().getHttpBaseUrl() + TEST_JSON_URL)))
            .then()
            .statusCode(HttpStatus.SC_OK);
    }

    private void firstStubUrlenc() {
        wm.stubFor(post(urlEqualTo(TEST_URLENC_URL))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\": \"{{randomValue type='UUID'}}\"}")
            )
            .withServeEventListener("recordState",
                Parameters.from(
                    Map.of(
                        "context", "{{jsonPath response.body '$.id'}}",
                        "state", Map.of(
                            "id", "{{jsonPath response.body '$.id'}}",
                            "clientId", "{{formData request.body 'form' urlDecode=true}}{{form.clientId}}",
                            "amount", "{{formData request.body 'form' urlDecode=true}}{{form.amount}}"
                        )
                    )
                )
            )
        );
    }

    private void firstStubJson() {
        wm.stubFor(post(urlEqualTo(TEST_JSON_URL))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\": \"{{randomValue type='UUID'}}\"}")
            )
            .withServeEventListener("recordState",
                Parameters.from(
                    Map.of(
                        "context", "{{jsonPath response.body '$.id'}}",
                        "state", Map.of(
                            "id", "{{jsonPath response.body '$.id'}}",
                            "clientId", "{{jsonPath request.body '$.clientId'}}",
                            "amount", "{{jsonPath request.body '$.amount'}}"
                        )
                    )
                )
            )
        );
    }

    private void secondStubUrlenc() {
        wm.stubFor(post(urlEqualTo(TEST_URLENC_URL))
                .andMatching("state-matcher",
                    Parameters.from(Map.of(
                        "hasContext", "{{formData request.body 'form' urlDecode=true}}{{form.id}}",
                        "numericComparisons", Map.of("amount", Map.of(
                            "lte", "{{#assign 'contextId'}}{{formData request.body 'form' urlDecode=true}}{{form.id}}{{/assign}}{{state context=contextId property='amount'}}",
                            "gt", "500"
                            ))
                        )
                    )
                )
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"operationId\": \"{{randomValue type='UUID'}}\"}")
            )
        );
    }

    private void secondStubJson() {
        wm.stubFor(post(urlEqualTo(TEST_JSON_URL))
            .andMatching("state-matcher",
                Parameters.from(Map.of(
                        "hasContext", "{{jsonPath response.body '$.id'}}",
                        "numericComparisons", Map.of("amount", Map.of(
                            "lte", "{{#assign 'contextId'}}{{jsonPath response.body '$.id'}}{{/assign}}{{state context=contextId property='amount'}}",
                            "gt", "500"
                        ))
                    )
                )
            )
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"operationId\": \"{{randomValue type='UUID'}}\"}")
            )
        );
    }
}