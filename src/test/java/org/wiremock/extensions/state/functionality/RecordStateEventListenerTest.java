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
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
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
        var body = new HashMap<>(Map.of("contextValueOne", contextValueOne));
        if (contextValueTwo != null) {
            body.put("contextValueTwo", contextValueTwo);
        }

        given()
            .accept(ContentType.JSON)
            .body(body
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
                                "stateValueTwoWithoutDefault", "{{jsonPath request.body '$.contextValueTwo'}}",
                                "stateValueTwoWithDefault", "{{jsonPath request.body '$.contextValueTwo' default='stateValueTwoDefaultValue'}}",
                                "previousStateValueTwo", "{{state context=request.pathSegments.[1] property='stateValueTwoWithoutDefault' default='noPrevious'}}"
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

            assertThat(contextManager.numUpdates(contextName)).isEqualTo(1);

            assertContext(contextName, contextName, "one", "one", "noPrevious");
        }

        @Test
        public void test_stateUsesNullOrDefaultIfNoValueIsMissingSpecified_ok() {

            var contextName = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", contextName, null);

            assertThat(contextManager.numUpdates(contextName)).isEqualTo(1);

            assertThat(contextManager.getContext(contextName))
                .isPresent()
                .hasValueSatisfying(it -> {
                        assertThat(it.getProperties())
                            .containsEntry("stateValueTwoWithoutDefault", "")
                            .containsEntry("stateValueTwoWithDefault", "stateValueTwoDefaultValue");
                    }
                );
        }

        @Test
        public void test_stateIsOverwritten_ok() {

            var contextName = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", contextName, "one");
            postRequest("state", contextName, "two");

            assertThat(contextManager.numUpdates(contextName)).isEqualTo(2);

            assertThat(contextManager.getContext(contextName))
                .isPresent()
                .hasValueSatisfying(it -> {
                        assertThat(it.getProperties())
                            .containsEntry("stateValueOne", contextName)
                            .containsEntry("stateValueTwoWithoutDefault", "two")
                            .containsEntry("stateValueTwoWithDefault", "two");
                    }
                );
        }

        @Test
        public void test_stateCanAccessPreviousState_ok() {

            var contextName = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", contextName, "one");
            postRequest("state", contextName, "two");

            assertThat(contextManager.numUpdates(contextName)).isEqualTo(2);

            assertThat(contextManager.getContext(contextName))
                .isPresent()
                .hasValueSatisfying(it -> {
                        assertThat(it.getProperties())
                            .containsEntry("stateValueOne", contextName)
                            .containsEntry("stateValueTwoWithoutDefault", "two")
                            .containsEntry("stateValueTwoWithDefault", "two")
                            .containsEntry("previousStateValueTwo", "one");
                    }
                );
        }

        @Test
        public void test_otherStateIsWritten_ok() {
            var contextNameOne = RandomStringUtils.randomAlphabetic(5);
            var contextNameTwo = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", contextNameOne, "one");
            postRequest("state", contextNameTwo, "two");

            assertThat(contextManager.numUpdates(contextNameOne)).isEqualTo(1);
            assertThat(contextManager.numUpdates(contextNameTwo)).isEqualTo(1);

            assertContext(contextNameOne, contextNameOne, "one", "one", "noPrevious");
            assertContext(contextNameTwo, contextNameTwo, "two", "two", "noPrevious");
        }

        private void assertContext(String contextNameTwo, String stateValueOne, String stateValueTwoWithoutDefault, String stateValueTwoWithDefault, String statePrevious) {
            assertThat(contextManager.getContext(contextNameTwo))
                .isPresent()
                .hasValueSatisfying(it -> {
                        assertThat(it.getList()).isEmpty();
                        assertThat(it.getProperties())
                            .hasSize(4)
                            .containsEntry("stateValueOne", stateValueOne)
                            .containsEntry("stateValueTwoWithoutDefault", stateValueTwoWithoutDefault)
                            .containsEntry("stateValueTwoWithDefault", stateValueTwoWithDefault)
                            .containsEntry("previousStateValueTwo", statePrevious);
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

            assertThat(contextManager.numUpdates("last-" + contextName)).isEqualTo(1);

            assertContext("last-" + contextName, 1, 0, contextName, "one");
            assertContext("first-" + contextName, 1, 0, contextName, "one");
        }

        @Test
        public void test_stateIsAppended_ok() {

            var contextName = RandomStringUtils.randomAlphabetic(5);

            postRequest("list", contextName, "one");
            assertThat(contextManager.numUpdates("last-" + contextName)).isEqualTo(1);
            postRequest("list", contextName, "two");
            assertThat(contextManager.numUpdates("last-" + contextName)).isEqualTo(2);

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

            assertContextNumUpdates(context, 1);
        }

        @Test
        void test_multipleUpdateCount_increase_ok() {
            var context = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", context, "one");
            postRequest("state", context, "one");
            postRequest("state", context, "one");

            assertContextNumUpdates(context, 3);
        }

        @Test
        void test_differentContext_ok() {
            var contextOne = RandomStringUtils.randomAlphabetic(5);
            var contextTwo = RandomStringUtils.randomAlphabetic(5);

            postRequest("state", contextOne, "one");
            postRequest("state", contextTwo, "one");
            postRequest("state", contextOne, "one");

            assertContextNumUpdates(contextOne, 2);
            assertContextNumUpdates(contextTwo, 1);
        }

        @Disabled
        @DisplayName("update count only increased by one when both property and list are updated")
        @Test
        void test_updatePropertyAndList_incOne() {
            throw new NotImplementedException();
        }
    }
}