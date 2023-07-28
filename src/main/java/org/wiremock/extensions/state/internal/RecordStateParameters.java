package org.wiremock.extensions.state.internal;

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
