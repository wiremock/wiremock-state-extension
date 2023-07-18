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

import com.github.tomakehurst.wiremock.store.Store;

import java.util.Map;
import java.util.Optional;

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

    public Optional<Context> getContext(String contextName) {
        synchronized (store) {
            return store.get(contextName).map(it -> (Context) it);
        }
    }

    public void deleteContext(String contextName) {
        synchronized (store) {
            store.remove(contextName);
        }
    }

    public Long createOrUpdateContext(String contextName, Map<String, String> properties) {
        synchronized (store) {
            var context = store.get(contextName)
                .map(it -> (Context) it)
                .map(it -> {
                    it.incUpdateCount();
                    return it;
                }).orElseGet(() -> new Context(contextName));
            context.getProperties().putAll(properties);
            store.put(contextName, context);
            return context.getUpdateCount();
        }
    }

    public Long numUpdates(String contextName) {
        synchronized (store) {
            return store.get(contextName).map(it -> ((Context) it).getUpdateCount()).orElse(0L);
        }
    }

    public Long numReads(String contextName) {
        synchronized (store) {
            return store.get(contextName).map(it -> ((Context) it).getMatchCount()).orElse(0L);
        }
    }
}
