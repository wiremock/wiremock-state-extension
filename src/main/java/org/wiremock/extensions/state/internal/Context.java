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
package org.wiremock.extensions.state.internal;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private final String contextName;
    private Integer numUpdates = 1;

    private final Map<String, String> properties = new HashMap<>();

    public Context(String contextName) {
        this.contextName = contextName;
    }

    public String getContextName() {
        return contextName;
    }

    public Integer getNumUpdates() {
        return numUpdates;
    }

    public Integer incUpdates() {
        numUpdates = numUpdates + 1;
        return numUpdates;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "Context{" +
            "contextName='" + contextName + '\'' +
            ", numUpdates=" + numUpdates +
            ", properties=" + properties +
            '}';
    }
}
