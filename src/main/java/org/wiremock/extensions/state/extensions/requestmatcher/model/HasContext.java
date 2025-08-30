package org.wiremock.extensions.state.extensions.requestmatcher.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HasContext implements BaseRequestMatcher, BaseContextMatcher {
    private final String contextTemplate;

    public HasContext(@JsonProperty("hasContext") String contextTemplate) {
        this.contextTemplate = contextTemplate;
    }

    @Override
    public String getContextTemplate() {
        return contextTemplate;
    }

    @Override
    public String assertValid() {
        if(contextTemplate == null) {
            return "'hasContext' must be specified";
        } else {
            return null;
        }
    }
}
