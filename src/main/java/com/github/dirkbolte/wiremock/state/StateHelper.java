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
