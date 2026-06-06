package com.syncturtle.services.instance.messaging.db;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.syncturtle.services.instance.messaging.InstanceConfigEventPublisher;
import com.syncturtle.services.instance.messaging.db.event.InstanceConfigurationEventToPublish;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InstanceConfigurationEventRelay {

    private final InstanceConfigEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InstanceConfigurationEventToPublish wrapper) {
        publisher.publishInstanceConfigurationEvent(wrapper.getEvent());
    }

}
