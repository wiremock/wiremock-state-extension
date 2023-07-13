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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.store.Store;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(SAME_THREAD)
class StateExtensionExampleTest {

    private static final String TEST_URL = "/test";
    private static final Store<String, Object> store = new CaffeineStore();
    private static final ObjectMapper mapper = new ObjectMapper();

    @RegisterExtension
    public static WireMockExtension wm = WireMockExtension.newInstance()
        .options(
            wireMockConfig().dynamicPort().dynamicHttpsPort()
                .extensions(new StateExtension(store))
        )
        .build();


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
                                "id", "{{jsonPath response.body '$.id'}}",
                                "firstName", "{{jsonPath request.body '$.contextValue'}}",
                                "lastName", "{{jsonPath request.body '$.contextValue'}}"
                            )
                        )
                    )
                )
        );
    }

    private void createGetStub() throws JsonProcessingException {
        wm.stubFor(
            get(urlPathMatching(TEST_URL + "/[^/]+"))
                .willReturn(
                    WireMock.ok()
                        .withHeader("content-type", "application/json")
                        .withJsonBody(
                            mapper.readTree(
                                mapper.writeValueAsString(Map.of(
                                        "id", "{{state context=request.pathSegments.[1] property='id'}}",
                                        "firstName", "{{state context=request.pathSegments.[1] property='firstName'}}",
                                        "lastName", "{{state context=request.pathSegments.[1] property='lastName'}}"
                                    )
                                )
                            )
                        )
                )
        );
    }
}