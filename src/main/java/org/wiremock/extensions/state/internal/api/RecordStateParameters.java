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
package org.wiremock.extensions.state.internal.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordStateParameters {
    private String context;

    private Map<String, String> state;
    private ListParameters list;

    public ListParameters getList() {
        return list;
    }

    public void setList(ListParameters list) {
        this.list = list;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Map<String, String> getState() {
        return state;
    }

    public void setState(Map<String, String> state) {
        this.state = state;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListParameters {
        private Map<String, String> addFirst;
        private Map<String, String> addLast;

        public Map<String, String> getAddFirst() {
            return addFirst;
        }

        public void setAddFirst(Map<String, String> addFirst) {
            this.addFirst = addFirst;
        }

        public Map<String, String> getAddLast() {
            return addLast;
        }

        public void setAddLast(Map<String, String> addLast) {
            this.addLast = addLast;
        }
    }
}
