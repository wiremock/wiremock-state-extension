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

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.responsetemplating.RequestTemplateModel;
import com.github.tomakehurst.wiremock.extension.responsetemplating.TemplateEngine;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import org.wiremock.extensions.state.internal.Context;
import org.wiremock.extensions.state.internal.ContextManager;
import org.wiremock.extensions.state.internal.ContextTemplateModel;
import org.wiremock.extensions.state.internal.StateExtensionMixin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Request matcher for state.
 * <p>
 * DO NOT REGISTER directly. Use {@link org.wiremock.extensions.state.StateExtension} instead.
 *
 * @see org.wiremock.extensions.state.StateExtension
 */
public class StateRequestMatcher extends RequestMatcherExtension implements StateExtensionMixin {

    private final TemplateEngine templateEngine;
    private final ContextManager contextManager;

    public StateRequestMatcher(ContextManager contextManager, TemplateEngine templateEngine) {
        this.contextManager = contextManager;
        this.templateEngine = templateEngine;
    }

    private static List<Map.Entry<CountMatcher, Object>> getMatches(Parameters parameters) {
        return parameters
            .entrySet()
            .stream()
            .filter(it -> CountMatcher.from(it.getKey()) != null)
            .map(it -> Map.entry(CountMatcher.from(it.getKey()), it.getValue()))
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String getName() {
        return "state-matcher";
    }

    @Override
    public MatchResult match(Request request, Parameters parameters) {
        Map<String, Object> model = new HashMap<>(Map.of("request", RequestTemplateModel.from(request)));
        return Optional
            .ofNullable(parameters.getString("hasContext", null))
            .map(template -> hasContext(model, parameters, template))
            .or(() -> Optional.ofNullable(parameters.getString("hasNotContext", null)).map(template -> hasNotContext(model, template)))
            .orElseThrow(() -> createConfigurationError("Parameters should only contain 'hasContext' or 'hasNotContext'"));
    }

    private MatchResult hasContext(Map<String, Object> model, Parameters parameters, String template) {
        return contextManager.getContext(renderTemplate(model, template))
            .map(context -> {
                List<Map.Entry<CountMatcher, Object>> matchers = getMatches(parameters);
                if (matchers.isEmpty()) {
                    return MatchResult.exactMatch();
                } else {
                    return calculateMatch(model, context, matchers);
                }
            }).orElseGet(MatchResult::noMatch);
    }

    private MatchResult calculateMatch(Map<String, Object> model, Context context, List<Map.Entry<CountMatcher, Object>> matchers) {
        model.put("context", ContextTemplateModel.from(context));
        var result = matchers
            .stream()
            .map(it -> it.getKey().evaluate(context, Long.valueOf(renderTemplate(model, it.getValue().toString()))))
            .filter(it -> !it)
            .count();

        return MatchResult.partialMatch((double) result / matchers.size());
    }

    private MatchResult hasNotContext(Map<String, Object> model, String template) {
        var context = renderTemplate(model, template);
        if (contextManager.getContext(context).isEmpty()) {
            return MatchResult.exactMatch();
        } else {
            return MatchResult.noMatch();
        }
    }

    String renderTemplate(Object context, String value) {
        return templateEngine.getUncachedTemplate(value).apply(context);
    }

    private enum CountMatcher {
        updateCountEqualTo((Context context, Long value) -> context.getUpdateCount().equals(value)),
        updateCountLessThan((Context context, Long value) -> context.getUpdateCount() < value),
        updateCountMoreThan((Context context, Long value) -> context.getUpdateCount() > value);

        private final BiFunction<Context, Long, Boolean> evaluator;

        CountMatcher(BiFunction<Context, Long, Boolean> evaluator) {
            this.evaluator = evaluator;
        }

        public static CountMatcher from(String from) {
            return Arrays.stream(values()).filter(it -> it.name().equals(from)).findFirst().orElse(null);
        }

        public boolean evaluate(Context context, Long value) {
            return this.evaluator.apply(context, value);
        }
    }
}
