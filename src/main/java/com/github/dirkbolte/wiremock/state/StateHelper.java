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
package com.github.dirkbolte.wiremock.state;

import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.helpers.HandlebarsHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class StateHelper extends HandlebarsHelper<Object> {

    private final StateRecordingAction recordingAction;

    public StateHelper(StateRecordingAction recordingAction) {
        this.recordingAction = recordingAction;
    }

    @Override
    public Object apply(Object o, Options options) {
        String context = options.hash("context");
        String property = options.hash("property");
        if (StringUtils.isEmpty(context)) {
            return handleError("The context cannot be empty");
        }
        if (StringUtils.isEmpty(property)) {
            return handleError("The property cannot be empty");
        }

        return Optional.ofNullable(recordingAction.getState(context, property))
            .orElse(handleError(String.format("No state for context %s, property %s found", context, property)));
    }
}
