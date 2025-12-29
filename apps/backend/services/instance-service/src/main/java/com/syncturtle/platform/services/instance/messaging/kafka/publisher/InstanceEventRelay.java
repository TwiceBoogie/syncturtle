package com.syncturtle.platform.services.instance.messaging.kafka.publisher;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.syncturtle.platform.services.instance.payload.InstanceEventToPublish;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class InstanceEventRelay {

    private final KafkaInstanceEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InstanceEventToPublish e) {
        publisher.publishInstanceEvent(e.event());
    }
}
