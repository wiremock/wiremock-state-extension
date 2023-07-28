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
package org.wiremock.extensions.state;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.Parameters;
import io.restassured.http.ContentType;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RecordStateEventListenerTest extends AbstractTestBase {

    @BeforeEach
    void setup() {
        createStatePostStub();
        createListPostStub();
    }

    private void postRequest(String path, String contextValueOne, String contextValueTwo) {
        given()
            .accept(ContentType.JSON)
            .body(Map.of(
                "contextValueOne", contextValueOne,
                "contextValueTwo", contextValueTwo)
            )
            .post(assertDoesNotThrow(() -> new URI(wm.getRuntimeInfo().getHttpBaseUrl() + "/" + path + "/" + contextValueOne)))
            .then()
            .statusCode(HttpStatus.SC_OK);
    }

    private void createStatePostStub() {
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
    }

    private void createListPostStub() {
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
                            "context", "{{join 'first' request.pathSegments.[1] '-'}}",
                            "list", Map.of(
                                "addFirst", Map.of(
                                    "stateValueOne", "{{jsonPath request.body '$.contextValueOne'}}",
                                    "stateValueTwo", "{{jsonPath request.body '$.contextValueTwo'}}"
                                )
                            )
                        )
                    )
                )
                .withServeEventListener(
                    "recordState",
                    Parameters.from(
                        Map.of(
                            "context", "{{join 'last' request.pathSegments.[1] '-'}}",
                            "list", Map.of(
                                "addLast", Map.of(
                                    "stateValueOne", "{{jsonPath request.body '$.contextValueOne'}}",
                                    "stateValueTwo", "{{jsonPath request.body '$.contextValueTwo'}}"
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
        public void test_stateIsWritten_ok() {

            var contextName = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", contextName, "one");

            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.numUpdates(contextName)).isEqualTo(1));

            assertContext(contextName, contextName, "one");
        }

        @Test
        public void test_stateIsOverwritten_ok() {

            var contextName = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", contextName, "one");
            postRequest("state", contextName, "two");

            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.numUpdates(contextName)).isEqualTo(2));

            assertContext(contextName, contextName, "two");
        }

        @Test
        public void test_otherStateIsWritten_ok() {
            var contextNameOne = RandomStringUtils.randomAlphabetic(5);
            var contextNameTwo = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", contextNameOne, "one");
            postRequest("state", contextNameTwo, "two");

            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.numUpdates(contextNameOne)).isEqualTo(1));
            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.numUpdates(contextNameTwo)).isEqualTo(1));

            assertContext(contextNameOne, contextNameOne, "one");
            assertContext(contextNameTwo, contextNameTwo, "two");
        }

        private void assertContext(String contextNameTwo, String stateValueOne, String stateValueTwo) {
            assertThat(contextManager.getContext(contextNameTwo))
                .isPresent()
                .hasValueSatisfying(it -> {
                        assertThat(it.getList()).isEmpty();
                        assertThat(it.getProperties())
                            .hasSize(2)
                            .containsEntry("stateValueOne", stateValueOne)
                            .containsEntry("stateValueTwo", stateValueTwo);
                    }
                );
        }
    }

    @Nested
    public class List {
        @Test
        public void test_stateIsWritten_ok() {

            var contextName = RandomStringUtils.randomAlphabetic(5);

            postRequest("list", contextName, "one");

            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.numUpdates("last-" + contextName)).isEqualTo(1));

            assertContext("last-" + contextName, 1, 0, contextName, "one");
            assertContext("first-" + contextName, 1, 0, contextName, "one");
        }

        @Test
        public void test_stateIsAppended_ok() {

            var contextName = RandomStringUtils.randomAlphabetic(5);

            postRequest("list", contextName, "one");
            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.numUpdates("last-" + contextName)).isEqualTo(1));
            postRequest("list", contextName, "two");
            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.numUpdates("last-" + contextName)).isEqualTo(2));

            assertContext("last-" + contextName, 2, 0, contextName, "one");
            assertContext("last-" + contextName, 2, 1, contextName, "two");
            assertContext("first-" + contextName, 2, 0, contextName, "two");
            assertContext("first-" + contextName, 2, 1, contextName, "one");
        }

        private void assertContext(String contextNameTwo, Integer size, Integer index, String stateValueOne, String stateValueTwo) {
            assertThat(contextManager.getContext(contextNameTwo))
                .isPresent()
                .hasValueSatisfying(it -> {
                        assertThat(it.getList())
                            .hasSize(size)
                            .satisfies(list ->
                                assertThat(list.get(index))
                                    .isNotNull()
                                    .containsEntry("stateValueOne", stateValueOne)
                                    .containsEntry("stateValueTwo", stateValueTwo)
                            );
                        assertThat(it.getProperties()).isEmpty();
                    }
                );
        }
    }

    @Nested
    public class NumUpdates {
        @Test
        void test_unknownContextCount_0_ok() {
            var context = RandomStringUtils.randomAlphabetic(5);

            assertThat(contextManager.numUpdates(context)).isEqualTo(0);
        }

        @Test
        void test_initialUpdateCount_1_ok() {
            var context = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", context, "one");

            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(contextManager.numUpdates(context)).isEqualTo(1));
        }

        @Test
        void test_multipleUpdateCount_increase_ok() {
            var context = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", context, "one");
            postRequest("state", context, "one");
            postRequest("state", context, "one");

            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(contextManager.numUpdates(context)).isEqualTo(3));
        }

        @Test
        void test_differentContext_ok() {
            var contextOne = RandomStringUtils.randomAlphabetic(5);
            var contextTwo = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", contextOne, "one");
            postRequest("state", contextTwo, "one");
            postRequest("state", contextOne, "one");

            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(contextManager.numUpdates(contextOne)).isEqualTo(2));
            await()
                .pollInterval(Duration.ofMillis(10))
                .atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(contextManager.numUpdates(contextTwo)).isEqualTo(1));
        }

    }
}