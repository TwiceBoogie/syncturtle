package com.syncturtle.common.spring.autoconfigure.observability;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

@AutoConfiguration
@ConditionalOnClass({ OpenTelemetry.class, OpenTelemetryAppender.class })
@ConditionalOnBean(OpenTelemetry.class)
@ConditionalOnProperty(prefix = "app.observability.otel.logback", name = "enabled", havingValue = "true", matchIfMissing = false)
public class OpenTelemetryLogbackAutoConfiguration {

    @Bean(name = "otelLogbackAppenderInstalled")
    @ConditionalOnMissingBean(name = "otelLogbackAppenderInstalled")
    InitializingBean otelLogbackAppenderInstalled(OpenTelemetry openTelemetry) {
        return () -> OpenTelemetryAppender.install(openTelemetry);
    }
}
