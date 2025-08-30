package org.wiremock.extensions.state.extensions.requestmatcher.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Not implements BaseRequestMatcher {
    private final BaseRequestMatcher baseRequestMatcher;

    public Not(@JsonProperty("not") BaseRequestMatcher baseRequestMatcher) {
        this.baseRequestMatcher = baseRequestMatcher;
    }

    @Override
    public String assertValid() {
        if (baseRequestMatcher == null) {
            return "'not' must be specified";
        } else {
            return null;
        }
    }

    public BaseRequestMatcher getBaseRequestMatcher() {
        return baseRequestMatcher;
    }
}
