package org.wiremock.extensions.state.extensions.requestmatcher.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HasNotContext implements BaseRequestMatcher, BaseContextMatcher {
    private final String contextTemplate;

    public HasNotContext(@JsonProperty("hasNotContext") String contextTemplate) {
        this.contextTemplate = contextTemplate;
    }

    @Override
    public String getContextTemplate() {
        return contextTemplate;
    }

    @Override
    public String assertValid() {
        if(contextTemplate == null) {
            return "'hasNotContext' must be specified";
        } else {
            return null;
        }
    }
}
