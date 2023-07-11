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
