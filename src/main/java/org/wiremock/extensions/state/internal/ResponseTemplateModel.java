/*
 * Copyright (C) 2023 Dirk Bolte
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wiremock.extensions.state.internal;

import com.github.tomakehurst.wiremock.common.ListOrSingle;
import com.github.tomakehurst.wiremock.http.LoggedResponse;

import java.util.Map;
import java.util.stream.Collectors;

public final class ResponseTemplateModel {
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
