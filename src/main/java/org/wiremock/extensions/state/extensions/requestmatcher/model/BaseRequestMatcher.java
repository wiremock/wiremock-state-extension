package org.wiremock.extensions.state.extensions.requestmatcher.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
    @JsonSubTypes.Type(HasContext.class),
    @JsonSubTypes.Type(HasNotContext.class),
    @JsonSubTypes.Type(Not.class),
    @JsonSubTypes.Type(Or.class),
    @JsonSubTypes.Type(And.class)
})
public interface BaseRequestMatcher {

    String assertValid();
}
