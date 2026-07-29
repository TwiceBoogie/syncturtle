package com.syncturtle.services.instance.scheduler;

import java.lang.management.ManagementFactory;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.services.instance.service.OutboxRelayService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.outbox.relay", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {

    private final OutboxRelayService relay;
    private final String workerId;

    public OutboxRelayScheduler(OutboxRelayService relay) {
        Assert.notNull(relay, "outbox relay is required");

        this.relay = relay;
        this.workerId = buildWorkerId();
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay.fixed-delay-ms:500}", initialDelayString = "${app.outbox.relay.initial-delay-ms:2000}")
    public void publishDueOutboxMessages() {
        try {
            relay.relayDueMessages(workerId);
        } catch (Exception exception) {
            log.error("Outbox relay cycle failed. workerId={} message={}", workerId, exception.getMessage(), exception);
        }
    }

    private static String buildWorkerId() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        String uniqueSuffix = UUID.randomUUID().toString();

        return runtimeName + ":" + uniqueSuffix;
    }

}
