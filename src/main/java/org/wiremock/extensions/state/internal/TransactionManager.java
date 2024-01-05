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
import org.wiremock.extensions.state.internal.model.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class TransactionManager {

    private final String TRANSACTION_KEY_PREFIX = "transaction:";
    private final Store<String, Object> store;

    public TransactionManager(Store<String, Object> store) {
        this.store = store;
    }

    public void withTransaction(String requestId, String contextName, Consumer<Transaction> consumer) {
        var transactionKey = createTransactionKey(requestId);
        synchronized (store) {
            @SuppressWarnings("unchecked") var requestTransactions = store.get(transactionKey).map(it -> (Map<String, Transaction>) it).orElse(new HashMap<>());
            var contextTransaction = requestTransactions.getOrDefault(contextName, new Transaction(contextName));
            try {
                consumer.accept(contextTransaction);
            } finally {
                requestTransactions.put(contextName, contextTransaction);
                store.put(transactionKey, requestTransactions);
            }
        }
    }

    public void deleteTransaction(String requestId, String contextName) {
        var transactionKey = createTransactionKey(requestId);
        synchronized (store) {
            @SuppressWarnings("unchecked") var requestTransactions = store.get(transactionKey).map(it -> (Map<String, Transaction>) it).orElse(new HashMap<>());
            requestTransactions.remove(contextName);
        }
    }

    public Set<String> getContextNamesByRequestId(String requestId) {
        var transactionKey = createTransactionKey(requestId);
        synchronized (store) {
            @SuppressWarnings("unchecked") var requestTransactions = store.get(transactionKey).map(it -> (Map<String, Transaction>) it).orElse(new HashMap<>());
            return requestTransactions.keySet();
        }
    }

    private String createTransactionKey(String requestId) {
        return TRANSACTION_KEY_PREFIX + requestId;
    }
}
