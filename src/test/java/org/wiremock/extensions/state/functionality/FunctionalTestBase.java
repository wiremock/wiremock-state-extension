package org.wiremock.extensions.state.functionality;

import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.store.Store;
import com.github.tomakehurst.wiremock.store.files.FileSourceBlobStore;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.wiremock.extensions.state.TestBase;
import org.wiremock.extensions.state.internal.BlobToContextStoreAdapter;
import org.wiremock.extensions.state.internal.Context;
import org.wiremock.extensions.state.internal.ContextManager;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@Execution(SAME_THREAD)
public class FunctionalTestBase extends TestBase {

    protected static ContextManager contextManager;

    @BeforeAll
    static void setupAll() {
        Store<String, Context> store = new BlobToContextStoreAdapter(
            new FileSourceBlobStore(new SingleRootFileSource(rootDir.resolve("state").toFile()))
        );
        contextManager = new ContextManager(store);
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
