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

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ServeEventListener;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.wiremock.extensions.state.internal.ContextManager;
import org.wiremock.extensions.state.internal.StateExtensionMixin;
import org.wiremock.extensions.state.internal.TransactionManager;

/**
 * Persist transaction-related information in the context.
 * <p>
 * DO NOT REGISTER directly. Use {@link org.wiremock.extensions.state.StateExtension} instead.
 *
 * @see org.wiremock.extensions.state.StateExtension
 */
public class TransactionEventListener implements ServeEventListener, StateExtensionMixin {

    private final TransactionManager transactionManager;


    public TransactionEventListener(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public String getName() {
        return "stateTransaction";
    }

    @Override
    public boolean applyGlobally() {
        return true;
    }

    @Override
    public void afterComplete(ServeEvent serveEvent, Parameters parameters) {

        String requestId = serveEvent.getId().toString();
        var contextNames = transactionManager.getContextNamesByRequestId(requestId);
        contextNames.forEach((contextName) -> transactionManager.deleteTransaction(requestId, contextName));
    }
}
