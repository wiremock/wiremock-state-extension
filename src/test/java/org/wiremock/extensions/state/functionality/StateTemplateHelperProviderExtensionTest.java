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
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class StateTemplateHelperProviderExtensionTest extends AbstractTestBase {

    private void postContext(String contextName, Map<String, Object> body) {
        var preparedBody = new HashMap<>(body);
        preparedBody.put("contextName", contextName);
        given()
            .contentType(ContentType.JSON)
            .body(preparedBody)
            .post(assertDoesNotThrow(() -> new URI(String.format("%s/%s", wm.getRuntimeInfo().getHttpBaseUrl(), "contexturl"))))
            .then()
            .statusCode(HttpStatus.SC_OK);
    }

    private void getContext(String contextName, Consumer<Map<String, Object>> assertion) {
        Map<String, Object> result = given()
            .accept(ContentType.JSON)
            .get(assertDoesNotThrow(() -> new URI(String.format("%s/%s/%s", wm.getRuntimeInfo().getHttpBaseUrl(), "contexturl", contextName))))
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().as(mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
        assertion.accept(result);
    }

    private void getContextList(String contextName, Consumer<List<Map<String, Object>>> assertion) {
        List<Map<String, Object>> result = given()
            .accept(ContentType.JSON)
            .get(assertDoesNotThrow(() -> new URI(String.format("%s/%s/%s", wm.getRuntimeInfo().getHttpBaseUrl(), "contexturl", contextName))))
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().body().as(
                mapper.getTypeFactory().constructCollectionLikeType(ArrayList.class,
                    mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class))
            );
        assertion.accept(result);
    }

    private void createContextListPostStub(Map<String, Object> stateConfiguration) {
        wm.stubFor(
            post(urlEqualTo("/contexturl"))
                .willReturn(WireMock.ok().withHeader("content-type", "application/json"))
                .withServeEventListener(
                    "recordState",
                    Parameters.from(
                        Map.of(
                            "context", "{{jsonPath request.body '$.contextName'}}",
                            "list", Map.of("addLast", stateConfiguration)
                        )
                    )
                )
        );
    }

    private void createContextGetStub(Map<String, String> body) {
        wm.stubFor(
            get(urlPathMatching("/contexturl/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(Json.node(Json.write(body)))
                )
        );
    }

    private void createContextGetStub(String body) {
        wm.stubFor(
            get(urlPathMatching("/contexturl/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withBody(body)
                )
        );
    }

    private void createContextStatePostStub(Map<String, Object> stateConfiguration) {

        wm.stubFor(
            post(urlEqualTo("/contexturl"))
                .willReturn(WireMock.ok().withHeader("content-type", "application/json"))
                .withServeEventListener(
                    "recordState",
                    Parameters.from(
                        Map.of(
                            "context", "{{jsonPath request.body '$.contextName'}}",
                            "state", stateConfiguration
                        )
                    )
                )
        );
    }

    private void createContextGetStubWithBodyFile(String file) {
        wm.stubFor(
            get(urlPathMatching("/contexturl/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withBodyFile("StateTemplateHelperProviderExtensionTest/" + file)
                )
        );
    }

    @DisplayName("with no extension usage")
    @Nested
    public class NoExtension {

        @DisplayName("does not impact response creation or stub matching")
        @Test
        public void test_noExtensionUsage_ok() throws JsonProcessingException, URISyntaxException {
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
    }

    @DisplayName("with missing context")
    @Nested
    public class MissingContext {

        private final String contextName = "unknownContext";

        @DisplayName("when no default is specified returns empty string")
        @Test
        void test_hasDefaultEmptyString() {
            createContextGetStub(Map.of("contextValue", "{{state context=request.pathSegments.[1] property='contextValue'}}"));

            getContext(contextName, (result) -> assertThat(result).containsEntry("contextValue", ""));
        }

        @DisplayName("when default is specified returns default")
        @Test
        void test_usesDefault() {
            createContextGetStub(Map.of("contextValue", "{{state context=request.pathSegments.[1] property='contextValue' default='aDefaultValue'}}"));

            getContext(contextName, (result) -> assertThat(result).containsEntry("contextValue", "aDefaultValue"));
        }

        @DisplayName("no default allows creating null-return with handlebars")
        @Test
        void test_allowNull() {
            createContextGetStubWithBodyFile("value_or_null.json");

            getContext(contextName, (result) -> assertThat(result).containsEntry("contextValue", null));
        }

        @DisplayName("no default allows creating ignore-return with handlebars")
        @Test
        void test_allowIgnore() {
            createContextGetStubWithBodyFile("value_or_ignore.json");

            getContext(contextName, (result) -> assertThat(result).doesNotContainKey("contextValue"));
        }

        @DisplayName("when accessing list property")
        @Nested
        public class FullList {
            @DisplayName("interprets empty default")
            @Test
            void test_listEmptyDefault() {
                createContextGetStubWithBodyFile("list_with_empty_default.json");

                getContextList(contextName, (result) -> assertThat(result).isEmpty());
            }

            @DisplayName("interprets filled default")
            @Test
            void test_listFilledDefault() {
                createContextGetStubWithBodyFile("list_with_filled_default.json");

                getContextList(contextName, (result) -> assertThat(result).containsExactly(Map.of("listValue", "defaultListValue")));
            }

            @DisplayName("has proper built-in default")
            @Test
            void test_listNoDefault() {
                createContextGetStubWithBodyFile("list_with_no_default.json");

                getContextList(contextName, (result) -> assertThat(result).isEmpty());
            }
        }
    }

    @DisplayName("with missing property")
    @Nested
    public class MissingProperty {
        private final String contextName = "knownContext";

        @BeforeEach
        public void setup() {
            createContextStatePostStub(Map.of());
            postContext(contextName, Map.of());
            assertThat(contextManager.getContext(contextName)).isPresent();
        }

        @DisplayName("when no default is specified returns empty string")
        @Test
        void test_hasDefaultEmptyString() {
            createContextGetStub(Map.of("contextValue", "{{state context=request.pathSegments.[1] property='contextValue'}}"));

            getContext(contextName, (result) -> assertThat(result).containsEntry("contextValue", ""));
        }

        @DisplayName("when default is specified returns default")
        @Test
        void test_usesDefault() {
            createContextGetStub(Map.of("contextValue", "{{state context=request.pathSegments.[1] property='contextValue' default='aDefaultValue'}}"));

            getContext(contextName, (result) -> assertThat(result).containsEntry("contextValue", "aDefaultValue"));
        }

        @DisplayName("no default allows creating null-return with handlebars")
        @Test
        void test_allowNull() {
            createContextGetStubWithBodyFile("value_or_null.json");

            getContext(contextName, (result) -> assertThat(result).containsEntry("contextValue", null));
        }

        @DisplayName("no default allows creating ignore-return with handlebars")
        @Test
        void test_allowIgnore() {
            createContextGetStubWithBodyFile("value_or_ignore.json");

            getContext(contextName, (result) -> assertThat(result).doesNotContainKey("contextValue"));
        }
    }

    @DisplayName("with missing list")
    @Nested
    public class MissingList {
        private final String contextName = "knownContext";

        @BeforeEach
        public void setup() {
            createContextStatePostStub(Map.of());
            postContext(contextName, Map.of());
            assertThat(contextManager.getContext(contextName)).isPresent();
        }

        @DisplayName("when accessing first element")
        @Nested
        public class FirstElement {

            @DisplayName("when no default is specified returns empty string")
            @Test
            void test_hasDefaultEmptyString() {
                createContextGetStub(Map.of("listValue", "{{state context=request.pathSegments.[1] list='[0].listValue'}}"));

                getContext(contextName, (result) -> assertThat(result).containsEntry("listValue", ""));
            }

            @DisplayName("when default is specified returns default")
            @Test
            void test_usesDefault() {
                createContextGetStub(Map.of("listValue", "{{state context=request.pathSegments.[1] property='[0].listValue' default='aDefaultValue'}}"));

                getContext(contextName, (result) -> assertThat(result).containsEntry("listValue", "aDefaultValue"));
            }
        }

        @DisplayName("when accessing index element")
        @Nested
        public class IndexElement {

            @DisplayName("when no default is specified returns empty string")
            @Test
            void test_hasDefaultEmptyString() {
                createContextGetStub(Map.of("listValue", "{{state context=request.pathSegments.[1] list='[1].listValue'}}"));

                getContext(contextName, (result) -> assertThat(result).containsEntry("listValue", ""));
            }

            @DisplayName("when default is specified returns default")
            @Test
            void test_usesDefault() {
                createContextGetStub(Map.of("listValue", "{{state context=request.pathSegments.[1] property='[1].listValue' default='aDefaultValue'}}"));

                getContext(contextName, (result) -> assertThat(result).containsEntry("listValue", "aDefaultValue"));
            }
        }

        @DisplayName("when accessing last element")
        @Nested
        public class LastElement {

            @DisplayName("when no default is specified returns empty string")
            @Test
            void test_hasDefaultEmptyString() {
                createContextGetStub(Map.of("listValue", "{{state context=request.pathSegments.[1] list='[-1].listValue'}}"));

                getContext(contextName, (result) -> assertThat(result).containsEntry("listValue", ""));
            }

            @DisplayName("when default is specified returns default")
            @Test
            void test_usesDefault() {
                createContextGetStub(Map.of("listValue", "{{state context=request.pathSegments.[1] property='[-1].listValue' default='aDefaultValue'}}"));

                getContext(contextName, (result) -> assertThat(result).containsEntry("listValue", "aDefaultValue"));
            }
        }

        @DisplayName("when accessing list property")
        @Nested
        public class FullList {
            @DisplayName("interprets empty default")
            @Test
            void test_listEmptyDefault() {
                createContextGetStubWithBodyFile("list_with_empty_default.json");

                getContextList(contextName, (result) -> assertThat(result).isEmpty());
            }

            @DisplayName("ignores filled default and takes empty list")
            @Test
            void test_listFilledDefault() {
                createContextGetStubWithBodyFile("list_with_filled_default.json");

                getContextList(contextName, (result) -> assertThat(result).isEmpty());
            }

            @DisplayName("has proper built-in default")
            @Test
            void test_listNoDefault() {
                createContextGetStubWithBodyFile("list_with_no_default.json");

                getContextList(contextName, (result) -> assertThat(result).isEmpty());
            }
        }
    }

    @DisplayName("with meta properties")
    @Nested
    public class MetaProperties {

        @DisplayName("when a context is present")
        @Nested
        public class Present {

        }

        @DisplayName("when a context is not present")
        @Nested
        public class NotPresent {

            private final String contextName = "unknownContext";

            @DisplayName("uses built-in default for updateCount")
            @Test
            public void test_updateCountNoDefault() {
                createContextGetStub(Map.of("updateCount", "{{state context=request.pathSegments.[1] property='updateCount'}}"));

                getContext(contextName, (result) -> assertThat(result).containsEntry("updateCount", "0"));
            }

            @DisplayName("uses specified default for updateCount when configured")
            @Test
            public void test_updateCountWithDefault() {
                createContextGetStub(Map.of("updateCount", "{{state context=request.pathSegments.[1] property='updateCount' default='5'}}"));

                getContext(contextName, (result) -> assertThat(result).containsEntry("updateCount", "5"));
            }

            @DisplayName("uses built-in default for listSize")
            @Test
            public void test_ListSizeNoDefault() {
                createContextGetStub(Map.of("listSize", "{{state context=request.pathSegments.[1] property='listSize'}}"));

                getContext(contextName, (result) -> assertThat(result).containsEntry("listSize", "0"));
            }

            @DisplayName("uses specified default for listSize when configured")
            @Test
            public void test_listSizeWithDefault() {
                createContextGetStub(Map.of("listSize", "{{state context=request.pathSegments.[1] property='listSize' default='5'}}"));

                getContext(contextName, (result) -> assertThat(result).containsEntry("listSize", "5"));
            }
        }
    }

    @DisplayName("with existing property")
    @Nested
    public class ExistingProperty {

        private final String contextName = "aContextName";

        @DisplayName("returns property from previous request")
        @Test
        void test_returnsState() {
            Map<String, Object> request = Map.of("contextValue", "aContextValue");
            createContextStatePostStub(Map.of("contextValue", "{{jsonPath request.body '$.contextValue'}}"));
            createContextGetStub(Map.of("contextValue", "{{state context=request.pathSegments.[1] property='contextValue'}}"));

            postContext(contextName, request);
            getContext(contextName, (result) -> assertThat(result).containsExactlyEntriesOf(request));
        }

        @DisplayName("with default specified returns property from previous request")
        @Test
        void test_withDefaultReturnsState() {
            Map<String, Object> request = Map.of("contextValue", "aContextValue");
            createContextStatePostStub(Map.of("contextValue", "{{jsonPath request.body '$.contextValue' default='defaultValue'}}"));
            createContextGetStub(Map.of("contextValue", "{{state context=request.pathSegments.[1] property='contextValue'}}"));

            postContext(contextName, request);
            getContext(contextName, (result) -> assertThat(result).containsExactlyEntriesOf(request));
        }

        @DisplayName("with multiple properties returns all specified")
        @Test
        void test_returnsMultipleStates() {
            Map<String, Object> request = Map.of("contextValueOne", "aContextValueOne", "contextValueTwo", "aContextValueTwo");
            createContextStatePostStub(Map.of(
                "contextValueOne", "{{jsonPath request.body '$.contextValueOne'}}",
                "contextValueTwo", "{{jsonPath request.body '$.contextValueTwo'}}"
            ));
            createContextGetStub(Map.of(
                "contextValueOne", "{{state context=request.pathSegments.[1] property='contextValueOne'}}",
                "contextValueTwo", "{{state context=request.pathSegments.[1] property='contextValueTwo'}}"
            ));

            postContext(contextName, request);
            getContext(contextName, (result) -> assertThat(result).containsAllEntriesOf(request));
        }

        @DisplayName("supports returning complete body")
        @Test
        void test_returnsCompleteBody() {
            Map<String, Object> request = Map.of("contextValue", "aContextValue", "nested", Map.of("a", "b"));
            createContextStatePostStub(Map.of("stateBody", "{{{jsonPath request.body '$'}}}"));
            createContextGetStub("{{{state context=request.pathSegments.[1] property='stateBody'}}}");

            postContext(contextName, request);
            getContext(contextName, (result) -> assertThat(result).containsAllEntriesOf(request));

        }

        @DisplayName("with multiple contexts returns correct state")
        @Test
        void test_returnsCorrectContext() {
            Map<String, Object> requestContextOne = Map.of("contextValue", "aContextValueOne");
            Map<String, Object> requestContextTwo = Map.of("contextValue", "aContextValueTwo");
            createContextStatePostStub(Map.of("contextValue", "{{jsonPath request.body '$.contextValue'}}"));
            createContextGetStub(Map.of("contextValue", "{{state context=request.pathSegments.[1] property='contextValue'}}"));

            postContext(contextName + "One", requestContextOne);
            postContext(contextName + "Two", requestContextTwo);
            getContext(contextName + "One", (result) -> assertThat(result).containsExactlyEntriesOf(requestContextOne));
            getContext(contextName + "Two", (result) -> assertThat(result).containsExactlyEntriesOf(requestContextTwo));
        }
    }

    @DisplayName("with existing list")
    @Nested
    public class ExistingList {

        private final String contextName = "aContextName";

        @DisplayName("with single list element returns first element")
        @Test
        void test_singleEntryReturnsFirstElement() {
            Map<String, Object> request = Map.of("listValue", "aListValue");
            createContextListPostStub(Map.of("listValue", "{{jsonPath request.body '$.listValue'}}"));
            createContextGetStub(Map.of("listValue", "{{state context=request.pathSegments.[1] list='[0].listValue'}}"));

            postContext(contextName, request);
            getContext(contextName, (result) -> assertThat(result).containsAllEntriesOf(request));
        }

        @DisplayName("with default on list value list element returns correct value")
        @Test
        void test_withDefaultSingleEntryReturnsFirstElement() {
            Map<String, Object> request = Map.of("listValue", "aListValue");
            createContextListPostStub(Map.of("listValue", "{{jsonPath request.body '$.listValue'}}"));
            createContextGetStub(Map.of("listValue", "{{state context=request.pathSegments.[1] list='[0].listValue' default='aDefaultValue'}}"));

            postContext(contextName, request);
            getContext(contextName, (result) -> assertThat(result).containsAllEntriesOf(request));
        }

        @DisplayName("with single list element returns last element")
        @Test
        void test_singleEntryReturnsLastElement() {
            Map<String, Object> request = Map.of("listValue", "aListValue");
            createContextListPostStub(Map.of("listValue", "{{jsonPath request.body '$.listValue'}}"));
            createContextGetStub(Map.of("listValue", "{{state context=request.pathSegments.[1] list='[-1].listValue'}}"));

            postContext(contextName, request);
            getContext(contextName, (result) -> assertThat(result).containsAllEntriesOf(request));
        }

        @DisplayName("with multiple list elements returns first element")
        @Test
        void test_multipleEntriesReturnsFirstElement() {
            Map<String, Object> request = Map.of("listValue", "aListValue1");
            createContextListPostStub(Map.of("listValue", "{{jsonPath request.body '$.listValue'}}"));
            createContextGetStub(Map.of("listValue", "{{state context=request.pathSegments.[1] list='[0].listValue'}}"));

            postContext(contextName, request);
            postContext(contextName, Map.of("listValue", "aListValue2"));
            postContext(contextName, Map.of("listValue", "aListValue3"));
            getContext(contextName, (result) -> assertThat(result).containsAllEntriesOf(request));
        }

        @DisplayName("with multiple list elements returns last element")
        @Test
        void test_multipleEntriesReturnsLastElement() {
            Map<String, Object> request = Map.of("listValue", "aListValue3");
            createContextListPostStub(Map.of("listValue", "{{jsonPath request.body '$.listValue'}}"));
            createContextGetStub(Map.of("listValue", "{{state context=request.pathSegments.[1] list='[-1].listValue'}}"));

            postContext(contextName, Map.of("listValue", "aListValue1"));
            postContext(contextName, Map.of("listValue", "aListValue2"));
            postContext(contextName, request);
            getContext(contextName, (result) -> assertThat(result).containsAllEntriesOf(request));
        }

        @DisplayName("with multiple list elements returns index element")
        @Test
        void test_multipleEntriesReturnsIndexElement() {
            Map<String, Object> request = Map.of("listValue", "aListValue2");
            createContextListPostStub(Map.of("listValue", "{{jsonPath request.body '$.listValue'}}"));
            createContextGetStub(Map.of("listValue", "{{state context=request.pathSegments.[1] list='[1].listValue'}}"));

            postContext(contextName, Map.of("listValue", "aListValue1"));
            postContext(contextName, request);
            postContext(contextName, Map.of("listValue", "aListValue3"));
            getContext(contextName, (result) -> assertThat(result).containsAllEntriesOf(request));
        }

        @DisplayName("when accessing full list")
        @Nested
        public class FullList {

            @DisplayName("with no default renders list with one element")
            @Test
            void test_oneElementNoDefault() {
                Map<String, Object> request = Map.of("listValue", "aListValue");
                createContextListPostStub(Map.of("listValue", "{{jsonPath request.body '$.listValue'}}"));
                createContextGetStubWithBodyFile("list_with_no_default.json");

                postContext(contextName, request);

                getContextList(contextName, (result) -> assertThat(result).containsExactly(request));
            }

            @DisplayName("with default renders list with one element")
            @Test
            void test_oneElementWithDefault() {
                Map<String, Object> request = Map.of("listValue", "aListValue");
                createContextListPostStub(Map.of("listValue", "{{jsonPath request.body '$.listValue'}}"));
                createContextGetStubWithBodyFile("list_with_filled_default.json");

                postContext(contextName, request);

                getContextList(contextName, (result) -> assertThat(result).containsExactly(request));
            }

            @DisplayName("renders list with multiple element")
            @Test
            void test_multipleElements() {
                Map<String, Object> requestOne = Map.of("listValue", "listValueOne");
                Map<String, Object> requestTwo = Map.of("listValue", "listValueTwo");
                Map<String, Object> requestThree = Map.of("listValue", "listValueThree");
                createContextListPostStub(Map.of("listValue", "{{jsonPath request.body '$.listValue'}}"));
                createContextGetStubWithBodyFile("list_with_no_default.json");

                postContext(contextName, requestOne);
                postContext(contextName, requestTwo);
                postContext(contextName, requestThree);

                getContextList(contextName, (result) -> assertThat(result).containsExactly(requestOne, requestTwo, requestThree));
            }
        }
    }
}