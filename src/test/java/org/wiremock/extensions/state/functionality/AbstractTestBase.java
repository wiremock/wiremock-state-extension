package org.wiremock.extensions.state.functionality;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.wiremock.extensions.state.CaffeineStore;
import org.wiremock.extensions.state.StateExtension;
import org.wiremock.extensions.state.internal.ContextManager;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(SAME_THREAD)
public class AbstractTestBase {
    protected static final ObjectMapper mapper = new ObjectMapper();
    protected static final CaffeineStore store = new CaffeineStore();
    protected static final ContextManager contextManager = new ContextManager(store);

    @RegisterExtension
    public static WireMockExtension wm = WireMockExtension.newInstance()
        .options(
            wireMockConfig().dynamicPort().dynamicHttpsPort().templatingEnabled(true).globalTemplating(true)
                .extensions(new StateExtension(store))
                .notifier(new ConsoleNotifier(true))
        )
        .build();

    @BeforeAll
    void setupAll() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeEach
    void setupBase() {
        wm.resetAll();
    }

    protected void assertContextNumUpdates(String context, int expected) {
        await()
            .pollInterval(Duration.ofMillis(10))
            .atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(contextManager.numUpdates(context)).isEqualTo(expected));
    }

}
