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
import com.github.tomakehurst.wiremock.extension.responsetemplating.RequestTemplateModel;
import com.github.tomakehurst.wiremock.extension.responsetemplating.TemplateEngine;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import org.wiremock.extensions.state.internal.ContextManager;

import java.util.Map;
import java.util.Optional;

/**
 * Request matcher for state.
 *
 * DO NOT REGISTER directly. Use {@link org.wiremock.extensions.state.StateExtension} instead.
 *
 * @see org.wiremock.extensions.state.StateExtension
 */
public class StateRequestMatcher extends RequestMatcherExtension {

    private final TemplateEngine templateEngine;
    private final ContextManager contextManager;

    public StateRequestMatcher(ContextManager contextManager, TemplateEngine templateEngine) {
        this.contextManager = contextManager;
        this.templateEngine = templateEngine;
    }

    @Override
    public String getName() {
        return "state-matcher";
    }

    @Override
    public MatchResult match(Request request, Parameters parameters) {
        if (parameters.size() != 1) {
            throw new ConfigurationException("Parameters should only contain one entry ('hasContext' or 'hasNotContext'");
        }
        var model = Map.of("request", RequestTemplateModel.from(request));
        return Optional
            .ofNullable(parameters.getString("hasContext", null))
            .map(template -> hasContext(model, template))
            .or(() -> Optional.ofNullable(parameters.getString("hasNotContext", null)).map(template -> hasNotContext(model, template)))
            .orElseThrow(() -> new ConfigurationException("Parameters should only contain 'hasContext' or 'hasNotContext'"));
    }

    private MatchResult hasContext(Map<String, RequestTemplateModel> model, String template) {
        var context = renderTemplate(model, template);
        if (contextManager.hasContext(context)) {
            return MatchResult.exactMatch();
        } else {
            return MatchResult.noMatch();
        }
    }

    private MatchResult hasNotContext(Map<String, RequestTemplateModel> model, String template) {
        var context = renderTemplate(model, template);
        if (!contextManager.hasContext(context)) {
            return MatchResult.exactMatch();
        } else {
            return MatchResult.noMatch();
        }
    }

    String renderTemplate(Object context, String value) {
        return templateEngine.getUncachedTemplate(value).apply(context);
    }
}
