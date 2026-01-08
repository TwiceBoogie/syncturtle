package com.syncturtle.platform.services.instance.services.telemetry;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.platform.services.instance.configurations.telemetry.ProductTelemetrySdkConfig.ProductTelemetrySdk;
import com.syncturtle.platform.services.instance.models.Instance;
import com.syncturtle.platform.services.instance.repositories.InstanceRepository;

import io.opentelemetry.api.trace.Tracer;

@Component
@ConditionalOnProperty(prefix = "app.product-telemetry", name = "enabled", havingValue = "true")
public class ProductTelemetryReporter {

    private final Tracer tracer;

    private final InstanceRepository instanceRepository;

    public ProductTelemetryReporter(ProductTelemetrySdk productTelemetrySdk,
            InstanceRepository instanceRepository) {
        this.tracer = productTelemetrySdk.getSdk().getTracer("syncturtle.product.telemetry");
        this.instanceRepository = instanceRepository;
    }

    @Scheduled(fixedDelayString = "${app.product-telemetry.interval:PT10M}")
    @Transactional(readOnly = true)
    public void emitSnapshot() {
        Instance instance = instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtAsc().orElse(null);

        if (instance == null) {
            return;
        }

        if (!instance.isTelemetryEnabled()) {
            return;
        }

        long userCount = 0;
        long workspaceCount = 0;

        tracer.spanBuilder("syncturtle.instance.snapshot")
                .setAttribute("syncturtle.instance_id", instance.getInstanceId())
                .setAttribute("syncturtle.edition", instance.getEdition().name())
                .setAttribute("syncturtle.current_version", instance.getUpdateCheck().getCurrentVersion())
                .setAttribute("syncturtle.latest_version", instance.getUpdateCheck().getLatestVersion())
                .setAttribute("syncturtle.support_required", instance.isSupportRequired())
                .setAttribute("syncturtle.setup_done", instance.isSetupDone())
                .setAttribute("syncturtle.verified", instance.isVerified())
                .setAttribute("syncturtle.domain", safe(instance.getRuntime().getDomain()))
                .setAttribute("syncturtle.is_test", instance.isTest())
                .setAttribute("syncturtle.last_checked_at", toIso(instance.getUpdateCheck().getLastCheckedAt()))
                .setAttribute("syncturtle.count.users", userCount)
                .setAttribute("syncturtle.count.workspaces", workspaceCount)
                .setNoParent()
                .startSpan()
                .end();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String toIso(Instant instant) {
        return instant == null ? "" : instant.toString();
    }
}
