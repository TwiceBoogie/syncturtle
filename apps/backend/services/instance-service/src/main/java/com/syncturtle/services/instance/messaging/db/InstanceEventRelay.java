package com.syncturtle.services.instance.messaging.db;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.syncturtle.services.instance.messaging.db.event.InstanceEventToPublish;
import com.syncturtle.services.instance.messaging.kafka.publisher.KafkaInstanceEventPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class InstanceEventRelay {

    private final KafkaInstanceEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InstanceEventToPublish wrapper) {
        publisher.publishInstanceEvent(wrapper.getEvent());
    }

}
