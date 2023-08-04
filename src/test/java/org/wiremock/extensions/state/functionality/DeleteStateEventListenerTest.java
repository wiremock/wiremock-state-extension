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
import com.github.tomakehurst.wiremock.extension.Parameters;
import io.restassured.http.ContentType;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.RestAssured.given;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class DeleteStateEventListenerTest extends AbstractTestBase {

    @BeforeEach
    void setup() {
        createGetStubState();
        createGetStubList();
        createPostStub();
    }

    private void getRequest(String path, String context) throws URISyntaxException {
        given()
            .accept(ContentType.JSON)
            .get(new URI(String.format("%s/%s/%s", wm.getRuntimeInfo().getHttpBaseUrl(), path, context)))
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract()
            .body()
            .jsonPath().get("value");
    }

    private void postRequest(String path, String contextName, String contextValueTwo) throws URISyntaxException {
        given()
            .accept(ContentType.JSON)
            .body(Map.of("contextValueOne", contextName))
            .body(Map.of("contextValueTwo", contextValueTwo))
            .post(new URI(wm.getRuntimeInfo().getHttpBaseUrl() + "/" + path + "/" + contextName))
            .then()
            .statusCode(HttpStatus.SC_OK);
    }

    private void createPostStub() {
        wm.stubFor(
            WireMock.post(urlPathMatching("/state/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withBody("{}")
                )
                .withServeEventListener(
                    "recordState",
                    Parameters.from(
                        Map.of(
                            "context", "{{request.pathSegments.[1]}}",
                            "state", Map.of(
                                "stateValueOne", "{{jsonPath request.body '$.contextValueOne'}}",
                                "stateValueTwo", "{{jsonPath request.body '$.contextValueTwo'}}"
                            )
                        )
                    )
                )
        );
        wm.stubFor(
            WireMock.post(urlPathMatching("/list/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withBody("{}")
                )
                .withServeEventListener(
                    "recordState",
                    Parameters.from(
                        Map.of(
                            "context", "{{request.pathSegments.[1]}}",
                            "list", Map.of(
                                "addLast", Map.of(
                                    "stateValueOne", "{{jsonPath request.body '$.contextValueOne'}}",
                                    "stateValueTwo", "{{jsonPath request.body '$.contextValueTwo'}}"
                                )
                            )
                        )
                    )
                ));
    }

    private void createGetStubState() {
        wm.stubFor(
            get(urlPathMatching("/state/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withBody("{}")
                )
                .withServeEventListener(
                    "deleteState",
                    Parameters.from(
                        Map.of(
                            "context", "{{request.pathSegments.[1]}}"
                        )
                    )
                )

        );
    }

    private void createGetStubList() {
        wm.stubFor(
            get(urlPathMatching("/list/deleteFirst/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withBody("{}")
                )
                .withServeEventListener(
                    "deleteState",
                    Parameters.from(
                        Map.of(
                            "context", "{{request.pathSegments.[2]}}",
                            "list", Map.of("deleteFirst", true)
                        )
                    )
                )
        );
        wm.stubFor(
            get(urlPathMatching("/list/deleteLast/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withBody("{}")
                )
                .withServeEventListener(
                    "deleteState",
                    Parameters.from(
                        Map.of(
                            "context", "{{request.pathSegments.[2]}}",
                            "list", Map.of("deleteLast", true)
                        )
                    )
                )
        );
        wm.stubFor(
            get(urlPathMatching("/list/deleteIndex/[^/]+/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withBody("{}")
                )
                .withServeEventListener(
                    "deleteState",
                    Parameters.from(
                        Map.of(
                            "context", "{{request.pathSegments.[3]}}",
                            "list", Map.of("deleteIndex", "{{request.pathSegments.[2]}}")
                        )
                    )
                )
        );
        wm.stubFor(
            get(urlPathMatching("/list/deleteWhere/[^/]+/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withBody("{}")
                )
                .withServeEventListener(
                    "deleteState",
                    Parameters.from(
                        Map.of(
                            "context", "{{request.pathSegments.[3]}}",
                            "list", Map.of("deleteWhere", Map.of(
                                    "property", "{{join 'state' 'Value' 'Two' ''}}",
                                    "value", "{{request.pathSegments.[2]}}"
                                )
                            )
                        )
                    )
                )
        );
    }

    @Nested
    public class State {

        @Test
        void test_unknownContext_noOtherContext_ok() throws URISyntaxException {
            var context = RandomStringUtils.randomAlphabetic(5);

            getRequest("state", context);

            await()
                .pollDelay(Duration.ofSeconds(1))
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.getContext(context)).isEmpty());
        }

        @Test
        void test_unknownContext_otherContext_ok() throws URISyntaxException {
            var context = RandomStringUtils.randomAlphabetic(5);
            var otherContext = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", otherContext, "one");
            getRequest("state", context);

            await()
                .pollDelay(Duration.ofSeconds(1))
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.getContext(context)).isEmpty());
            await()
                .pollDelay(Duration.ofSeconds(1))
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.getContext(otherContext)).isPresent());
        }

        @Test
        void test_knownContext_noOtherContext_ok() throws URISyntaxException {
            var context = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", context, "one");
            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.getContext(context)).isPresent());

            getRequest("state", context);
            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.getContext(context)).isEmpty());
        }

        @Test
        void test_knownContext_withOtherContext_ok() throws URISyntaxException {
            var context = RandomStringUtils.randomAlphabetic(5);
            var otherContext = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", context, "one");
            postRequest("state", otherContext, "one");

            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.getContext(context)).isPresent());
            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.getContext(otherContext)).isPresent());

            getRequest("state", context);
            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.getContext(context)).isEmpty());
            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.getContext(otherContext)).isPresent());
        }

    }

    @Nested
    public class List {
        @Test
        void test_unknownContext_noOtherContext_ok() throws URISyntaxException {
            var context = RandomStringUtils.randomAlphabetic(5);

            getRequest("list/deleteFirst", context);

            await()
                .pollDelay(Duration.ofSeconds(1))
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.getContext(context)).isEmpty());
        }

        @Test
        void test_deleteFirst_ok() throws URISyntaxException {
            var contextName = RandomStringUtils.randomAlphabetic(5);

            postRequest("list", contextName, "one");
            assertList(contextName, list -> assertThat(list).hasSize(1));
            postRequest("list", contextName, "two");
            assertList(contextName, list -> assertThat(list).hasSize(2));

            getRequest("list/deleteFirst", contextName);

            assertList(contextName,
                list ->
                    assertThat(list)
                        .hasSize(1)
                        .first()
                        .satisfies(it -> assertThat(it).containsEntry("stateValueTwo", "two"))
            );
        }

        @Test
        void test_deleteLast_ok() throws URISyntaxException {
            var contextName = RandomStringUtils.randomAlphabetic(5);

            postRequest("list", contextName, "one");
            assertList(contextName, list -> assertThat(list).hasSize(1));
            postRequest("list", contextName, "two");
            assertList(contextName, list -> assertThat(list).hasSize(2));

            getRequest("list/deleteLast", contextName);

            assertList(contextName,
                list ->
                    assertThat(list)
                        .hasSize(1)
                        .first()
                        .satisfies(it -> assertThat(it).containsEntry("stateValueTwo", "one"))
            );
        }

        @Test
        void test_deleteIndex_middle_ok() throws URISyntaxException {
            var contextName = RandomStringUtils.randomAlphabetic(5);

            postRequest("list", contextName, "one");
            assertContextNumUpdates(contextName, 1);
            postRequest("list", contextName, "two");
            assertContextNumUpdates(contextName, 2);
            postRequest("list", contextName, "three");
            assertContextNumUpdates(contextName, 3);
            assertList(contextName, list -> assertThat(list).hasSize(3));

            getRequest("list/deleteIndex/1", contextName);

            assertList(contextName,
                list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.get(0)).containsEntry("stateValueTwo", "one");
                    assertThat(list.get(1)).containsEntry("stateValueTwo", "three");
                }
            );
        }

        @Test
        void test_deleteIndex_last_ok() throws URISyntaxException {
            var contextName = RandomStringUtils.randomAlphabetic(5);

            postRequest("list", contextName, "one");
            assertContextNumUpdates(contextName, 1);
            postRequest("list", contextName, "two");
            assertContextNumUpdates(contextName, 2);
            postRequest("list", contextName, "three");
            assertContextNumUpdates(contextName, 3);
            assertList(contextName, list -> assertThat(list).hasSize(3));

            getRequest("list/deleteIndex/2", contextName);

            assertList(contextName,
                list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.get(0)).containsEntry("stateValueTwo", "one");
                    assertThat(list.get(1)).containsEntry("stateValueTwo", "two");
                }
            );
        }

        @Test
        void test_deleteWhere_middle_ok() throws URISyntaxException {
            var contextName = RandomStringUtils.randomAlphabetic(5);

            postRequest("list", contextName, "one");
            assertContextNumUpdates(contextName, 1);
            postRequest("list", contextName, "two");
            assertContextNumUpdates(contextName, 2);
            postRequest("list", contextName, "three");
            assertContextNumUpdates(contextName, 3);
            assertList(contextName, list -> assertThat(list).hasSize(3));

            getRequest("list/deleteWhere/two", contextName);

            assertList(contextName,
                list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.get(0)).containsEntry("stateValueTwo", "one");
                    assertThat(list.get(1)).containsEntry("stateValueTwo", "three");
                }
            );
        }

        @Test
        void test_deleteWhere_last_ok() throws URISyntaxException {
            var contextName = RandomStringUtils.randomAlphabetic(5);

            postRequest("list", contextName, "one");
            assertContextNumUpdates(contextName, 1);
            postRequest("list", contextName, "two");
            assertContextNumUpdates(contextName, 2);
            postRequest("list", contextName, "three");
            assertContextNumUpdates(contextName, 3);
            assertList(contextName, list -> assertThat(list).hasSize(3));

            getRequest("list/deleteWhere/three", contextName);

            assertList(contextName,
                list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.get(0)).containsEntry("stateValueTwo", "one");
                    assertThat(list.get(1)).containsEntry("stateValueTwo", "two");
                }
            );
        }

        private void assertList(String contextName, Consumer<LinkedList<Map<String, String>>> consumer) {
            await()
                .pollDelay(Duration.ofMillis(10))
                .pollInterval(Duration.ofMillis(10))
                .atMost(ofSeconds(5))
                .untilAsserted(() ->
                    assertThat(contextManager.getContext(contextName))
                        .isPresent()
                        .hasValueSatisfying(context -> consumer.accept(context.getList()))
                );
        }

    }
}