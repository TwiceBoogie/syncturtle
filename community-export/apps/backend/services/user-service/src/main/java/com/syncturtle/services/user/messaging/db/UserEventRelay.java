package com.syncturtle.services.user.messaging.db;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.syncturtle.services.user.messaging.kafka.publisher.KafkaUserEventPublisher;
import com.syncturtle.services.user.payload.UserEventToPublish;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class UserEventRelay {

    private final KafkaUserEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserEventToPublish e) {
        publisher.publishUserEvent(e.event());
    }

}
