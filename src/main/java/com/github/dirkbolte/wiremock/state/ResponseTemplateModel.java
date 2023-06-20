package com.github.dirkbolte.wiremock.state;

import com.github.tomakehurst.wiremock.common.ListOrSingle;
import com.github.tomakehurst.wiremock.http.LoggedResponse;

import java.util.Map;
import java.util.stream.Collectors;

final class ResponseTemplateModel {
    private final Map<String, ListOrSingle<String>> headers;
    private final String body;

    private ResponseTemplateModel(Map<String, ListOrSingle<String>> headers, String body) {
        this.headers = headers;
        this.body = body;
    }

    public static ResponseTemplateModel from(LoggedResponse response) {

        var headers = response
            .getHeaders()
            .keys()
            .stream()
            .collect(Collectors.toMap(
                it -> it,
                it -> ListOrSingle.of(
                    response.getHeaders()
                        .getHeader(it)
                        .values()
                ))
            );
        return new ResponseTemplateModel(headers, response.getBodyAsString());
    }

    public Map<String, ListOrSingle<String>> getHeaders() {
        return this.headers;
    }

    public String getBody() {
        return this.body;
    }
}
