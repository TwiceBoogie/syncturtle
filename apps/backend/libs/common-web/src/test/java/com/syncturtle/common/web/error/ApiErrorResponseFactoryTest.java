package com.syncturtle.common.web.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import com.syncturtle.common.web.property.ErrorResponseProperties;

class ApiErrorResponseFactoryTest {

    @Test
    void neverExposesDebugInProductionOrSetup() {
        ErrorResponseProperties properties = new ErrorResponseProperties(true, true, true, true, 30);
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("prod", "local");
        MockEnvironment setup = new MockEnvironment();
        setup.setActiveProfiles("setup");
        MockEnvironment docker = new MockEnvironment();
        docker.setActiveProfiles("docker", "local");

        assertThat(new ApiErrorResponseFactory(properties, production)
                .fromUnhandledException(new IllegalStateException("sensitive"), null).getDebug()).isNull();
        assertThat(new ApiErrorResponseFactory(properties, setup)
                .fromUnhandledException(new IllegalStateException("sensitive"), null).getDebug()).isNull();
        assertThat(new ApiErrorResponseFactory(properties, docker)
                .fromUnhandledException(new IllegalStateException("sensitive"), null).getDebug()).isNull();
    }

    @Test
    void requiresExplicitOptInInDevelopment() {
        MockEnvironment development = new MockEnvironment();
        development.setActiveProfiles("local");
        ErrorResponseProperties disabled = new ErrorResponseProperties(true, true, false, false, 30);
        ErrorResponseProperties enabled = new ErrorResponseProperties(true, true, true, false, 30);

        assertThat(new ApiErrorResponseFactory(disabled, development)
                .fromUnhandledException(new IllegalStateException("sample"), null).getDebug()).isNull();
        assertThat(new ApiErrorResponseFactory(enabled, development)
                .fromUnhandledException(new IllegalStateException("sample"), null).getDebug()).isNotNull();
    }

    @Test
    void boundsStackTraceLines() {
        assertThatThrownBy(() -> new ErrorResponseProperties(true, true, true, true, 51))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
