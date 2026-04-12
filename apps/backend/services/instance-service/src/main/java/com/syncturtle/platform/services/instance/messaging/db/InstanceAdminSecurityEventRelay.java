package com.syncturtle.platform.services.instance.messaging.db;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.syncturtle.platform.services.instance.messaging.InstanceAdminSecurityEventPublisher;
import com.syncturtle.platform.services.instance.payload.InstanceAdminSecurityEventToPublish;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InstanceAdminSecurityEventRelay {

    private final InstanceAdminSecurityEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InstanceAdminSecurityEventToPublish e) {
        publisher.publishInstanceAdminSecurityEvent(e.event());
    }

}
