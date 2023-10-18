package org.wiremock.extensions.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.wiremock.extensions.state.StateExtension;

import java.io.File;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class TestBase {

    protected static final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    protected static Path rootDir;

    @RegisterExtension
    public WireMockExtension wm = WireMockExtension.newInstance()
        .options(
            wireMockConfig()
                .dynamicPort()
                .withRootDirectory(rootDir.toString())
                .templatingEnabled(true)
                .globalTemplating(true)
                .extensions(new StateExtension())
                .notifier(new ConsoleNotifier(true))
        )
        .build();

    @BeforeAll
    static void initRootDir() {
        new File(rootDir.toFile(), "mappings").mkdirs();
        new File(rootDir.toFile(), "state").mkdirs();
    }
}
