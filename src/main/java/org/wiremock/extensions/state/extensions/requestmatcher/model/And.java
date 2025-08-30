package org.wiremock.extensions.state.extensions.requestmatcher.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class And implements BaseRequestMatcher {
    private final List<BaseRequestMatcher> baseRequestMatcher;

    public And(@JsonProperty("and") List<BaseRequestMatcher> baseRequestMatcher) {
        this.baseRequestMatcher = baseRequestMatcher;
    }

    @Override
    public String assertValid() {
        if (baseRequestMatcher == null) {
            return "'and' must be specified";
        } else {
            return null;
        }
    }

    public List<BaseRequestMatcher> getBaseRequestMatcher() {
        return baseRequestMatcher;
    }
}
