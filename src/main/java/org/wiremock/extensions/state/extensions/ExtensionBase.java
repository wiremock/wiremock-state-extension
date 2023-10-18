package org.wiremock.extensions.state.extensions;

import com.github.tomakehurst.wiremock.extension.WireMockServices;
import com.github.tomakehurst.wiremock.extension.responsetemplating.TemplateEngine;
import org.wiremock.extensions.state.internal.ContextManager;

public abstract class ExtensionBase {

    private final WireMockServices services;
    protected final ContextManager contextManager;

    public ExtensionBase(WireMockServices services, ContextManager contextManager) {
        this.services = services;
        this.contextManager = contextManager;
    }

    protected TemplateEngine getTemplateEngine() {
        return services.getTemplateEngine();
    }

    protected String renderTemplate(Object context, String value) {
        return getTemplateEngine().getUncachedTemplate(value).apply(context);
    }
}
