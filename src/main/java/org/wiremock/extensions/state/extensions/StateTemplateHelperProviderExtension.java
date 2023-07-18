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

import com.github.jknack.handlebars.Helper;
import com.github.tomakehurst.wiremock.extension.TemplateHelperProviderExtension;
import org.wiremock.extensions.state.internal.ContextManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Response template helper provider for state.
 * <p>
 * DO NOT REGISTER directly. Use {@link org.wiremock.extensions.state.StateExtension} instead.
 *
 * @see org.wiremock.extensions.state.StateExtension
 */
public class StateTemplateHelperProviderExtension implements TemplateHelperProviderExtension {

    private final Map<String, Helper<?>> stateTemplateHelpers = new HashMap<>();

    public StateTemplateHelperProviderExtension(ContextManager contextManager) {
        stateTemplateHelpers.put("state", new StateHandlerbarHelper(contextManager));
    }

    @Override
    public Map<String, Helper<?>> provideTemplateHelpers() {
        return stateTemplateHelpers;
    }

    @Override
    public String getName() {
        return "state";
    }
}
