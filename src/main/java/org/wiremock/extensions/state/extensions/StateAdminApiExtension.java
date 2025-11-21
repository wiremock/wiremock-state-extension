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
package org.wiremock.extensions.state.extensions;

import com.github.tomakehurst.wiremock.admin.AdminTask;
import com.github.tomakehurst.wiremock.admin.Router;
import com.github.tomakehurst.wiremock.extension.AdminApiExtension;
import com.github.tomakehurst.wiremock.extension.WireMockServices;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.wiremock.extensions.state.internal.ContextManager;
import org.wiremock.extensions.state.internal.StateExtensionMixin;
import org.wiremock.extensions.state.internal.model.Context;

import java.util.Optional;
import java.util.UUID;

/**
 * ADMIN API extension to show and manage context contents.
 * <p>
 * DO NOT REGISTER directly. Use {@link org.wiremock.extensions.state.StateExtension} instead.
 *
 * @see org.wiremock.extensions.state.StateExtension
 */
public class StateAdminApiExtension implements AdminApiExtension, StateExtensionMixin {

    private final WireMockServices wireMockServices;
    private final ContextManager contextManager;


    public StateAdminApiExtension(ContextManager contextManager, WireMockServices services) {
        this.contextManager = contextManager;
        this.wireMockServices = services;
    }

    @Override
    public String getName() {
        return "stateAdminApi";
    }


    @Override
    public void contributeAdminApiRoutes(Router router) {
        router.add(RequestMethod.GET, createPath("contexts"), getContexts());
        router.add(RequestMethod.GET, createPath("contexts/{context}"), getContext());
        router.add(RequestMethod.DELETE, createPath("contexts/{context}"), deleteContext());

    }

    private AdminTask getContexts() {
        return (admin, serveEvent, pathParams) -> ResponseDefinition.okForJson(contextManager.getAllContextNames());
    }

    private AdminTask getContext() {
        return (admin, serveEvent, pathParams) -> {
            Optional<Context> context = contextManager.getContextCopy(pathParams.get("context"));
            return context.map(ResponseDefinition::okForJson).orElseGet(ResponseDefinition::notFound);
        };
    }

    private AdminTask deleteContext() {
        return (admin, serveEvent, pathParams) -> {
            contextManager.deleteContext(UUID.randomUUID().toString(), pathParams.get("context"));
            return ResponseDefinition.noContent();
        };
    }

    private String createPath(String path) {
        return String.format("/state-extension/%s", path);
    }
}
