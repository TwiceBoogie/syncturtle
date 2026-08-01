package com.syncturtle.services.instance.scheduler;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.services.instance.configuration.telemetry.ProductTelemetrySdkConfiguration.ProductTelemetrySdk;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.repository.UserRepository;
import com.syncturtle.services.instance.repository.WorkspaceRepository;

import io.opentelemetry.api.trace.Tracer;

@Component
@Profile("!setup")
@ConditionalOnProperty(prefix = "app.product-telemetry", name = "enabled", havingValue = "true")
public class ProductTelemetryScheduler {

    private final Tracer tracer;

    private final InstanceRepository instanceRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    public ProductTelemetryScheduler(
            ProductTelemetrySdk productTelemetrySdk,
            InstanceRepository instanceRepository,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository) {
        this.tracer = productTelemetrySdk.getSdk().getTracer("syncturtle.product.telemetry");
        this.instanceRepository = instanceRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
    }

    @Scheduled(fixedDelayString = "${app.product-telemetry.interval:PT10M}")
    @Transactional(readOnly = true)
    public void emitSnapshot() {
        Instance instance = instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class)
                .orElse(null);

        if (instance == null) {
            return;
        }

        if (!instance.isTelemetryEnabled()) {
            return;
        }

        long userCount = userRepository.countByActiveTrue();
        long workspaceCount = workspaceRepository.countByDeletedAtIsNull();

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
