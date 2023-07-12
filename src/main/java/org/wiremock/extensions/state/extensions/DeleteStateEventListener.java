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

import com.github.tomakehurst.wiremock.core.ConfigurationException;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ServeEventListener;
import com.github.tomakehurst.wiremock.extension.responsetemplating.RequestTemplateModel;
import com.github.tomakehurst.wiremock.extension.responsetemplating.TemplateEngine;
import com.github.tomakehurst.wiremock.store.Store;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.apache.commons.lang3.StringUtils;
import org.wiremock.extensions.state.internal.ContextManager;
import org.wiremock.extensions.state.internal.ResponseTemplateModel;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Event listener to trigger state context deletion.
 *
 * DO NOT REGISTER directly. Use {@link org.wiremock.extensions.state.StateExtension} instead.
 *
 * @see org.wiremock.extensions.state.StateExtension
 */
public class DeleteStateEventListener implements ServeEventListener {

    private final TemplateEngine templateEngine;
    private final ContextManager contextManager;


    public DeleteStateEventListener(ContextManager contextManager, TemplateEngine templateEngine) {
        this.contextManager = contextManager;
        this.templateEngine = templateEngine;
    }

    public void afterComplete(ServeEvent serveEvent, Parameters parameters) {
        var model = Map.of(
            "request", RequestTemplateModel.from(serveEvent.getRequest()),
            "response", ResponseTemplateModel.from(serveEvent.getResponse())
        );
        var contextName = createContextName(model, parameters);
        contextManager.deleteContext(contextName);
    }

    @Override
    public String getName() {
        return "deleteState";
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    private String createContextName(Map<String, Object> model, Parameters parameters) {
        var rawContext = Optional.ofNullable(parameters.getString("context")).filter(StringUtils::isNotBlank).orElseThrow(() -> new ConfigurationException("no context specified"));
        String context = renderTemplate(model, rawContext);
        if (StringUtils.isBlank(context)) {
            throw new ConfigurationException("context is blank");
        }
        return context;
    }

    private String renderTemplate(Object context, String value) {
        return templateEngine.getUncachedTemplate(value).apply(context);
    }
}
