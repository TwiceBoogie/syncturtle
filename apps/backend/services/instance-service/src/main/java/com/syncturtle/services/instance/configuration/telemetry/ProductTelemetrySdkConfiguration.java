package com.syncturtle.services.instance.configuration.telemetry;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.services.instance.configuration.property.ProductTelemetryProperties;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

@Configuration(proxyBeanMethods = false)
public class ProductTelemetrySdkConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.product-telemetry", name = "enabled", havingValue = "true")
    ProductTelemetrySdk productTelemetryOpenTelemetry(ProductTelemetryProperties props) {
        OtlpGrpcSpanExporterBuilder exporter = OtlpGrpcSpanExporter.builder().setEndpoint(props.getEndpoint());

        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            exporter.addHeader("Authorization", "Bearer " + props.getApiKey());
        }

        Resource resource = Resource.getDefault().merge(Resource.create(Attributes.builder()
                .put("service.name", "syncturtle-product-telemetry")
                .put("service.namespace", "syncturtle")
                .put("deployment.environment", "local")
                .put("telemetry.mode", "product")
                .build()));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(exporter.build()).build())
                .build();

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();

        return new ProductTelemetrySdk(sdk, tracerProvider);
    }

    public static final class ProductTelemetrySdk implements AutoCloseable {
        private final OpenTelemetrySdk sdk;
        private final SdkTracerProvider tracerProvider;

        public ProductTelemetrySdk(OpenTelemetrySdk sdk, SdkTracerProvider tracerProvider) {
            this.sdk = sdk;
            this.tracerProvider = tracerProvider;
        }

        public OpenTelemetrySdk getSdk() {
            return sdk;
        }

        @Override
        public void close() {
            tracerProvider.shutdown();
        }
    }
}
