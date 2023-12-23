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

import org.wiremock.extensions.state.internal.model.Context;

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;

public class ExtensionLogger {

    private ExtensionLogger() {

    }

    public static ExtensionLogger logger() {
        return InstanceHolder.instance;
    }

    public void info(Context context, String message) {
        notifier().info(buildMessage(context.getContextName(), message));
    }

    public void error(Context context, String message) {
        notifier().error(buildMessage(context.getContextName(), message));
    }

    public void info(String contextName, String message) {
        notifier().info(buildMessage(contextName, message));
    }

    public void error(String contextName, String message) {
        notifier().error(buildMessage(contextName, message));
    }

    private String buildMessage(String contextName, String message) {
        return String.format("Context '%s': %s", contextName, message);
    }

    private static final class InstanceHolder {
        private static final ExtensionLogger instance = new ExtensionLogger();
    }

}
