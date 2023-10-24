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
import com.github.tomakehurst.wiremock.extension.responsetemplating.RequestTemplateModel;
import com.github.tomakehurst.wiremock.extension.responsetemplating.TemplateEngine;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.apache.commons.lang3.StringUtils;
import org.wiremock.extensions.state.internal.ContextManager;
import org.wiremock.extensions.state.internal.RecordStateParameters;
import org.wiremock.extensions.state.internal.ResponseTemplateModel;
import org.wiremock.extensions.state.internal.StateExtensionMixin;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.wiremock.extensions.state.internal.ExtensionLogger.logger;

/**
 * Event listener to trigger state context recording.
 * <p>
 * DO NOT REGISTER directly. Use {@link org.wiremock.extensions.state.StateExtension} instead.
 *
 * @see org.wiremock.extensions.state.StateExtension
 */
public class RecordStateEventListener implements ServeEventListener, StateExtensionMixin {

    private final TemplateEngine templateEngine;
    private final ContextManager contextManager;

    public RecordStateEventListener(ContextManager contextManager, TemplateEngine templateEngine) {
        this.contextManager = contextManager;
        this.templateEngine = templateEngine;
    }

    public void beforeResponseSent(ServeEvent serveEvent, Parameters parameters) {
        var model = Map.of(
            "request", RequestTemplateModel.from(serveEvent.getRequest()),
            "response", ResponseTemplateModel.from(serveEvent.getResponse())
        );
        var configuration = Json.mapToObject(parameters, RecordStateParameters.class);
        var contextName = createContextName(model, parameters);
        handleState(contextName, model, configuration);
        handleList(contextName, model, configuration);
    }

    @Override
    public String getName() {
        return "recordState";
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    private void handleState(String contextName, Map<String, Object> model, RecordStateParameters parameters) {
        Optional.ofNullable(parameters.getState())
            .ifPresent(configuration ->
                contextManager.createOrUpdateContextState(contextName, getPropertiesFromConfiguration(model, configuration))
            );
    }

    private Map<String, String> getPropertiesFromConfiguration(Map<String, Object> model, Map<String, String> configuration) {
        return configuration.entrySet()
            .stream()
            .map(entry -> Map.entry(entry.getKey(), renderTemplate(model, entry.getValue())))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void handleList(String contextName, Map<String, Object> model, RecordStateParameters parameters) {
        Optional.ofNullable(parameters.getList())
            .ifPresent(listConfiguration -> {
                    Optional.ofNullable(listConfiguration.getAddFirst())
                        .ifPresent(configuration -> addFirst(contextName, model, configuration));
                    Optional.ofNullable(listConfiguration.getAddLast())
                        .ifPresent(configuration -> addLast(contextName, model, configuration));
                }
            );
    }

    private Long addFirst(String contextName, Map<String, Object> model, Map<String, String> configuration) {
        return contextManager.createOrUpdateContextList(contextName, list -> {
            list.addFirst(getPropertiesFromConfiguration(model, configuration));
            logger().info(contextName, "list::addFirst");
        });
    }

    private Long addLast(String contextName, Map<String, Object> model, Map<String, String> configuration) {
        return contextManager.createOrUpdateContextList(contextName, list -> {
            list.addLast(getPropertiesFromConfiguration(model, configuration));
            logger().info(contextName, "list::addLast");
        });
    }

    private String createContextName(Map<String, Object> model, Parameters parameters) {
        var rawContext = Optional.ofNullable(parameters.getString("context"))
            .filter(StringUtils::isNotBlank)
            .orElseThrow(() -> new ConfigurationException("no context specified"));
        String context = renderTemplate(model, rawContext);
        if (StringUtils.isBlank(context)) {
            throw createConfigurationError("context cannot be blank");
        }
        return context;
    }

    private String renderTemplate(Object context, String value) {
        return templateEngine.getUncachedTemplate(value).apply(context);
    }
}
