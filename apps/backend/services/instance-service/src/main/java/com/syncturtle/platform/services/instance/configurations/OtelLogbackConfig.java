package com.syncturtle.platform.services.instance.configurations;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

@Configuration
public class OtelLogbackConfig {

    @Bean
    InitializingBean installOtelLogbackAppender(OpenTelemetry openTelemetry) {
        return () -> OpenTelemetryAppender.install(openTelemetry);
    }
}
