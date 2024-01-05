package org.wiremock.extensions.state.functionality;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wiremock.extensions.state.StandaloneStateExtension;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

public class StandaloneExtensionTest {


    @RegisterExtension
    public static WireMockExtension wm = WireMockExtension.newInstance()
        .options(
            wireMockConfig().dynamicPort().dynamicHttpsPort().templatingEnabled(true).globalTemplating(true)
                .extensions(new StandaloneStateExtension())
        )
        .build();

    @Test
    public void test_initialized_ok() {
        assertThat(wm.getOptions().getDeclaredExtensions().getFactories()).anySatisfy(it -> {
            assertThat(it).isInstanceOf(StandaloneStateExtension.class);
        });
    }

}
