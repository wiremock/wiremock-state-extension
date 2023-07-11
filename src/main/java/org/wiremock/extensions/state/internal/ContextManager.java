package org.wiremock.extensions.state.internal;

import com.github.tomakehurst.wiremock.store.Store;

import java.util.Map;

public class ContextManager {

    private final Store<String, Object> store;

    public ContextManager(Store<String, Object> store) {
        this.store = store;
    }

    public Object getState(String contextName, String property) {
        synchronized (store) {
            return store.get(contextName).map(it -> ((Context) it).getProperties().get(property)).orElse(null);
        }
    }

    public boolean hasContext(String contextName) {
        synchronized (store) {
            return store.get(contextName).isPresent();
        }
    }

    public void deleteContext(String contextName) {
        synchronized (store) {
            store.remove(contextName);
        }
    }

    public Integer createOrUpdateContext(String contextName, Map<String, String> properties) {
        synchronized (store) {
            var context = store.get(contextName)
                .map(it -> (Context) it)
                .map(it -> {
                    it.incUpdates();
                    return it;
                }).orElseGet(() -> new Context(contextName));
            context.getProperties().putAll(properties);
            store.put(contextName, context);
            return context.getNumUpdates();
        }
    }

    public Integer numUpdates(String contextName) {
        synchronized (store) {
            return store.get(contextName).map(it -> ((Context) it).getNumUpdates()).orElse(0);
        }
    }
}
