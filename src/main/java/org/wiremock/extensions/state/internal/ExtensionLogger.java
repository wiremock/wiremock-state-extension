package org.wiremock.extensions.state.internal;

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
