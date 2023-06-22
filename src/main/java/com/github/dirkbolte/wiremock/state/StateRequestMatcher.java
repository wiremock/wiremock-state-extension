package com.github.dirkbolte.wiremock.state;

import com.github.tomakehurst.wiremock.core.ConfigurationException;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.responsetemplating.RequestTemplateModel;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;

import java.util.Map;
import java.util.Optional;

public class StateRequestMatcher extends RequestMatcherExtension {

    private final StateRecordingAction recordingAction;

    public StateRequestMatcher(StateRecordingAction recordingAction) {
        this.recordingAction = recordingAction;
    }

    @Override
    public String getName() {
        return "state-matcher";
    }

    @Override
    public MatchResult match(Request request, Parameters parameters) {
        if (parameters.size() != 1) {
            throw new ConfigurationException("Parameters should only contain one entry ('hasContext' or 'hasNotContext'");
        }
        var model = Map.of("request", RequestTemplateModel.from(request));
        return Optional
            .ofNullable(parameters.getString("hasContext", null))
            .map(template -> hasContext(model, template))
            .or(() -> Optional.ofNullable(parameters.getString("hasNotContext", null)).map(template -> hasNotContext(model, template)))
            .orElseThrow(() -> new ConfigurationException("Parameters should only contain 'hasContext' or 'hasNotContext'"));

    }

    private MatchResult hasContext(Map<String, RequestTemplateModel> model, String template) {
        var context = recordingAction.renderTemplate(model, template);
        if (recordingAction.hasContext(context)) {
            return MatchResult.exactMatch();
        } else {
            return MatchResult.noMatch();
        }
    }

    private MatchResult hasNotContext(Map<String, RequestTemplateModel> model, String template) {
        var context = recordingAction.renderTemplate(model, template);
        if (!recordingAction.hasContext(context)) {
            return MatchResult.exactMatch();
        } else {
            return MatchResult.noMatch();
        }
    }
}
