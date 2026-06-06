package com.syncturtle.services.workspace.messaging.db;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.syncturtle.services.workspace.messaging.db.event.WorkspaceEventToPublish;
import com.syncturtle.services.workspace.messaging.kafka.publisher.WorkspaceEventPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class WorkspaceEventRelay {

    private final WorkspaceEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(WorkspaceEventToPublish wrapper) {
        publisher.publishWorkspaceEvent(wrapper.getEvent());
    }

}
