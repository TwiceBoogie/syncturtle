package com.syncturtle.common.observability.autoconfigure;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

@AutoConfiguration(afterName = {
        "org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration",
        "org.springframework.boot.opentelemetry.autoconfigure.logging.OpenTelemetryLoggingAutoConfiguration",
        "org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingAutoConfiguration"
})
@ConditionalOnClass({
        OpenTelemetry.class,
        OpenTelemetryAppender.class
})
@ConditionalOnBean(OpenTelemetry.class)
@ConditionalOnProperty(name = { "management.opentelemetry.enabled", "management.logging.export.enabled",
        "management.logging.export.otlp.enabled" }, havingValue = "true", matchIfMissing = true)
public final class OpenTelemetryLogbackAutoConfiguration {

    @Bean(name = "otelLogbackAppenderInstalled")
    @ConditionalOnMissingBean(name = "otelLogbackAppenderInstalled")
    InitializingBean otelLogbackAppenderInstalled(
            OpenTelemetry openTelemetry) {
        return () -> OpenTelemetryAppender.install(openTelemetry);
    }
}