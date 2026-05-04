package com.syncturtle.services.instance.services.telemetry;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.syncturtle.services.instance.events.InstanceRegisteredEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.product-telemetry", name = "enabled", havingValue = "true")
public class ProductTelemetryOnRegisterListener {

    private final ProductTelemetryReporter reporter;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistered(InstanceRegisteredEvent event) {
        reporter.emitSnapshot();
    }
}
