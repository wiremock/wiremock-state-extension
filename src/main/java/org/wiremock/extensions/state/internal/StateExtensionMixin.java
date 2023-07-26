package org.wiremock.extensions.state.internal;

import com.github.tomakehurst.wiremock.core.ConfigurationException;
import com.github.tomakehurst.wiremock.extension.Extension;

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;

public interface StateExtensionMixin extends Extension {

    default ConfigurationException createConfigurationError(String format, String... message) {
        var msg = String.format(format, (Object[]) message);
        var prefixed = String.format("%s: %s", getName(), msg);
        notifier().error(prefixed);
        return new ConfigurationException(prefixed);
    }

}
