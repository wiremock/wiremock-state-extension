package org.wiremock.extensions.state.extensions.requestmatcher.model;

import com.github.tomakehurst.wiremock.extension.Parameters;

import java.util.Map;

public interface BaseContextMatcher {
    String getContextTemplate();

    default Parameters unmappedFields() {
        return Parameters.empty();
    }

}
