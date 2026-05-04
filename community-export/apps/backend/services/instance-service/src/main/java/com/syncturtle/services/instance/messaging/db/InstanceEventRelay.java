package com.syncturtle.services.instance.messaging.db;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.syncturtle.services.instance.messaging.kafka.publisher.KafkaInstanceEventPublisher;
import com.syncturtle.services.instance.payload.InstanceEventToPublish;

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
