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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Context {

    private final String contextName;
    private final Map<String, String> properties;
    private final LinkedList<Map<String, String>> list;
    private final List<String> requests;
    private Long updateCount = 1L;
    private Long matchCount = 0L;

    public Context(Context other) {
        this.contextName = other.contextName;

        this.properties = new LinkedHashMap<>();
        this.properties.putAll(other.properties);

        this.list = new LinkedList<>();
        this.list.addAll(other.list.stream().map(HashMap::new).collect(Collectors.toList()));

        this.requests = new LinkedList<>();
        this.requests.addAll(other.requests);

        this.updateCount = other.updateCount;
        this.matchCount = other.matchCount;
    }

    public Context(String contextName) {
        this.contextName = contextName;
        this.properties = new LinkedHashMap<>();
        this.list = new LinkedList<>();
        this.requests = new LinkedList<>();
    }

    @JsonCreator
    public Context(
        @JsonProperty("contextName") String contextName,
        @JsonProperty("properties") Map<String, String> properties,
        @JsonProperty("list") LinkedList<Map<String, String>> list,
        @JsonProperty("updateCount") Long updateCount,
        @JsonProperty("matchCount") Long matchCount
    ) {
        this.contextName = contextName;
        this.properties = properties;
        this.list = list;
        this.updateCount = updateCount;
        this.matchCount = matchCount;

        this.requests = new LinkedList<>();
    }

    public String getContextName() {
        return contextName;
    }

    public Long getUpdateCount() {
        return updateCount;
    }

    public Long getMatchCount() {
        return matchCount;
    }

    public Long incUpdateCount() {
        updateCount = updateCount + 1;
        return updateCount;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public LinkedList<Map<String, String>> getList() {
        return list;
    }

    @Override
    public String toString() {
        return "Context{" +
            "contextName='" + contextName + '\'' +
            ", properties=" + properties +
            ", list=" + list +
            ", updateCount=" + updateCount +
            '}';
    }
}
