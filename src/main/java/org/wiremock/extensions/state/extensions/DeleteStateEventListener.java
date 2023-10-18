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

import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.ConfigurationException;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ServeEventListener;
import com.github.tomakehurst.wiremock.extension.WireMockServices;
import com.github.tomakehurst.wiremock.extension.responsetemplating.RequestTemplateModel;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.apache.commons.lang3.StringUtils;
import org.wiremock.extensions.state.internal.ContextManager;
import org.wiremock.extensions.state.internal.DeleteStateParameters;
import org.wiremock.extensions.state.internal.ResponseTemplateModel;
import org.wiremock.extensions.state.internal.StateExtensionMixin;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Event listener to trigger state context deletion.
 * <p>
 * DO NOT REGISTER directly. Use {@link org.wiremock.extensions.state.StateExtension} instead.
 *
 * @see org.wiremock.extensions.state.StateExtension
 */
public class DeleteStateEventListener extends ExtensionBase implements ServeEventListener, StateExtensionMixin {

    public DeleteStateEventListener(WireMockServices services, ContextManager contextManager) {
        super(services, contextManager);
    }

    @Override
    public String getName() {
        return "deleteState";
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    public void beforeResponseSent(ServeEvent serveEvent, Parameters parameters) {
        var model = Map.of(
            "request", RequestTemplateModel.from(serveEvent.getRequest()),
            "response", ResponseTemplateModel.from(serveEvent.getResponse())
        );
        var configuration = Json.mapToObject(parameters, DeleteStateParameters.class);
        var contextName = createContextName(model, configuration);
        Optional.ofNullable(configuration.getList()).ifPresentOrElse(
            listConfig -> handleListDeletion(listConfig, contextName, model),
            () -> contextManager.deleteContext(contextName)
        );
    }

    private void handleListDeletion(DeleteStateParameters.ListParameters listConfig, String contextName, Map<String, Object> model) {
        if (Boolean.TRUE.equals(listConfig.getDeleteFirst())) {
            contextManager.createOrUpdateContextList(contextName, maps -> {
                if (!maps.isEmpty()) maps.removeFirst();
            });
        } else if (Boolean.TRUE.equals(listConfig.getDeleteLast())) {
            contextManager.createOrUpdateContextList(contextName, maps -> {
                if (!maps.isEmpty()) maps.removeLast();
            });
        } else if (StringUtils.isNotBlank(listConfig.getDeleteIndex())) {
            try {
                var index = Integer.parseInt(renderTemplate(model, listConfig.getDeleteIndex()));
                contextManager.createOrUpdateContextList(contextName, list -> list.remove(index));
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                throw createConfigurationError("List index '%s' does not exist or cannot be parsed: %s", listConfig.getDeleteIndex(), e.getMessage());
            }
        } else if (listConfig.getDeleteWhere() != null &&
            listConfig.getDeleteWhere().getProperty() != null &&
            listConfig.getDeleteWhere().getValue() != null
        ) {
            var property = renderTemplate(model, listConfig.getDeleteWhere().getProperty());
            var value = renderTemplate(model, listConfig.getDeleteWhere().getValue());
            contextManager.createOrUpdateContextList(contextName, list -> {
                var iterator = list.iterator();
                while (iterator.hasNext()) {
                    var element = iterator.next();
                    if (Objects.equals(element.getOrDefault(property, null), value)) {
                        iterator.remove();
                        break;
                    }
                }
            });
        } else {
            throw createConfigurationError("Missing/invalid configuration for list");
        }
    }

    private String createContextName(Map<String, Object> model, DeleteStateParameters parameters) {
        var rawContext = Optional.ofNullable(parameters.getContext()).filter(StringUtils::isNotBlank).orElseThrow(() -> new ConfigurationException("no context specified"));
        String context = renderTemplate(model, rawContext);
        if (StringUtils.isBlank(context)) {
            throw createConfigurationError("Context cannot be blank");
        }
        return context;
    }

}
