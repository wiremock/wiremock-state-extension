package org.wiremock.extensions.state.extensions.requestmatcher.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.extension.Parameters;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HasContext implements BaseRequestMatcher, BaseContextMatcher {
    private final String contextTemplate;
    private final Parameters unmappedFields = Parameters.empty();

    public HasContext(@JsonProperty("hasContext") String contextTemplate) {
        this.contextTemplate = contextTemplate;
    }

    @JsonAnySetter
    public void setUnmappedField(String key, Object value) {
        unmappedFields.put(key, value);
    }

    @Override
    public String getContextTemplate() {
        return contextTemplate;
    }

    @Override
    public String assertValid() {
        if (contextTemplate == null) {
            return "'hasContext' must be specified";
        } else {
            return null;
        }
    }

    @Override
    public Parameters unmappedFields() {
        return unmappedFields;
    }
}
