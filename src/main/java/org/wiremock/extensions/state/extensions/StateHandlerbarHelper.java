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

import com.fasterxml.jackson.core.JsonParser;
import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.helpers.HandlebarsHelper;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.StringUtils;
import org.wiremock.extensions.state.internal.ContextManager;

/**
 * Response templating helper to access state.
 * <p>
 * DO NOT REGISTER directly. Use {@link org.wiremock.extensions.state.StateExtension} instead.
 *
 * @see org.wiremock.extensions.state.StateExtension
 */
public class StateHandlerbarHelper extends HandlebarsHelper<Object> {

    private final ContextManager contextManager;

    public StateHandlerbarHelper(ContextManager contextManager) {
        this.contextManager = contextManager;
    }

    @Override
    public Object apply(Object o, Options options) {
        String contextName = options.hash("context");
        String property = options.hash("property");
        String list = options.hash("list");
        if (StringUtils.isEmpty(contextName)) {
            return handleError("'context' cannot be empty");
        }
        if (StringUtils.isBlank(property) && StringUtils.isBlank(list)) {
            return handleError("Either 'property' or 'list' has to be set");
        }
        if (StringUtils.isNotBlank(property) && StringUtils.isNotBlank(list)) {
            return handleError("Either 'property' or 'list' has to be set");
        }
        if (StringUtils.isNotBlank(property)) {
            return getProperty(contextName, property);
        } else {
            return getList(contextName, list);
        }
    }

    private Object getProperty(String contextName, String property) {
        return contextManager.getContext(contextName)
            .map(context -> {
                    if ("updateCount".equals(property)) {
                        return context.getUpdateCount();
                    } else if ("listSize".equals(property)) {
                        return context.getList().size();
                    } else {
                        return context.getProperties().get(property);
                    }
                }
            ).orElse(handleError(String.format("No state for context %s, property %s found", contextName, property)));
    }
    private Object getList(String contextName, String list) {
        return contextManager.getContext(contextName)
            .map(context -> JsonPath.read(context.getList(), list))
            .orElse(handleError(String.format("No state for context %s, list %s found", contextName, list)));
    }
}
