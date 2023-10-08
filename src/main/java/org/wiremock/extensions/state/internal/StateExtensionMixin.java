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
