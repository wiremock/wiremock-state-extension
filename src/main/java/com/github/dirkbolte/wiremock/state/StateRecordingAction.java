package com.github.dirkbolte.wiremock.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.core.ConfigurationException;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.extension.responsetemplating.RequestTemplateModel;
import com.github.tomakehurst.wiremock.extension.responsetemplating.TemplateEngine;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class StateRecordingAction extends PostServeAction {

    private static final int DEFAULT_EXPIRATION_SECONDS = 60 * 60;
    private final TemplateEngine templateEngine;

    private final Cache<String, Object> cache;

    public StateRecordingAction() {
        this(0);
    }

    @JsonCreator
    public StateRecordingAction(int expirationSeconds) {
        this.templateEngine = new TemplateEngine(Collections.emptyMap(), null, Collections.emptySet());

        var builder = Caffeine.newBuilder();
        if (expirationSeconds == 0) {
            builder.expireAfterWrite(Duration.ofSeconds(DEFAULT_EXPIRATION_SECONDS));
        } else {
            builder.expireAfterWrite(Duration.ofSeconds(expirationSeconds));
        }
        cache = builder.build();
    }

    @Override
    public void doAction(ServeEvent serveEvent, Admin admin, Parameters parameters) {
        var model = Map.of(
            "request", RequestTemplateModel.from(serveEvent.getRequest()),
            "response", ResponseTemplateModel.from(serveEvent.getResponse())
        );
        var context = createContext(model, parameters);
        storeState(context, model, parameters);
    }

    @Override
    public String getName() {
        return "recordState";
    }

    public Object getState(String context, String property) {
        return cache.getIfPresent(calculateKey(context, property));
    }

    private void storeState(String context, Map<String, Object> model, Parameters parameters) {
        @SuppressWarnings("unchecked") Map<String, Object> state = Optional.ofNullable(parameters.get("state"))
            .filter(it -> it instanceof Map)
            .map(Map.class::cast)
            .orElseThrow(() -> new ConfigurationException("no state specified"));
        state.entrySet()
            .stream()
            .map(entry -> Map.entry(entry.getKey(), renderTemplate(model, entry.getValue().toString())))
            .forEach(entry -> storeState(context, entry.getKey(), entry.getValue()));
    }

    private String createContext(Map<String, Object> model, Parameters parameters) {
        var rawContext = Optional.ofNullable(parameters.getString("context")).filter(StringUtils::isNotBlank).orElseThrow(() -> new ConfigurationException("no context specified"));
        String context = renderTemplate(model, rawContext);
        if (StringUtils.isBlank(context)) {
            throw new ConfigurationException("context is blank");
        }
        return context;
    }

    private String renderTemplate(Object context, String value) {
        return templateEngine.getUncachedTemplate(value).apply(context);
    }

    private void storeState(String context, String property, Object value) {
        cache.put(calculateKey(context, property), value);
    }

    private String calculateKey(String context, String property) {
        return String.format("%s,%s", context, property);
    }
}
