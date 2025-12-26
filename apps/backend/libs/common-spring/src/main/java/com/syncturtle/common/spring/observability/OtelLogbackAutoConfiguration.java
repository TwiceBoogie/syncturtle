package com.syncturtle.common.spring.observability;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ OpenTelemetry.class, OpenTelemetryAppender.class })
public class OtelLogbackAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "otelLogbackAppenderInstalled")
    InitializingBean otelLogbackAppenderInstalled(OpenTelemetry openTelemetry) {
        return () -> OpenTelemetryAppender.install(openTelemetry);
    }
}
