package org.wiremock.extensions.state;

import com.github.jknack.handlebars.Helper;
import com.github.tomakehurst.wiremock.extension.TemplateHelperProviderExtension;
import com.github.tomakehurst.wiremock.store.Store;
import org.wiremock.extensions.state.internal.StateHandlerbarHelper;

import java.util.HashMap;
import java.util.Map;

public class StateTemplateHelperProviderExtension implements TemplateHelperProviderExtension {

    private final Map<String, Helper<?>>  stateTemplateHelpers = new HashMap<>();

    public StateTemplateHelperProviderExtension(Store<String, Object> store) {
        stateTemplateHelpers.put("state", new StateHandlerbarHelper(store));
    }
    @Override
    public Map<String, Helper<?>> provideTemplateHelpers() {
        return stateTemplateHelpers;
    }

    @Override
    public String getName() {
        return "state";
    }
}
