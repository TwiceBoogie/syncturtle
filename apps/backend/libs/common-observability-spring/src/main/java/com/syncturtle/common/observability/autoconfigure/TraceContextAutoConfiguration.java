package com.syncturtle.common.observability.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.syncturtle.common.observability.tracing.TraceContextPropagator;

import io.opentelemetry.api.OpenTelemetry;

@AutoConfiguration(afterName = "org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration")
@ConditionalOnClass(OpenTelemetry.class)
@ConditionalOnBean(OpenTelemetry.class)
public class TraceContextAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TraceContextPropagator traceContextPropagator(OpenTelemetry openTelemetry) {
        return new TraceContextPropagator(openTelemetry);
    }

}
