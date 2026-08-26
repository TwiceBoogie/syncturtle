package com.syncturtle.services.user.service.collaborator.outbox;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.services.user.model.OutboxMessage;
import com.syncturtle.services.user.repository.OutboxMessageRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxMessageRecorder {

    private final OutboxMessageRepository repository;
    private final Clock clock;

    @Transactional
    public void markPublished(UUID messageId) {
        OutboxMessage message = requireMessage(messageId);

        message.markPublished(clock);
    }

    @Transactional
    public void markFailed(UUID messageId, String errorMessage, Duration retryDelay) {
        Assert.notNull(retryDelay, "retryDelay is required");

        OutboxMessage message = requireMessage(messageId);

        message.markFailed(errorMessage, retryDelay, clock);
    }

    private OutboxMessage requireMessage(UUID messageId) {
        Assert.notNull(messageId, "messageId is required");

        return repository.findById(messageId)
                .orElseThrow(() -> new IllegalStateException("Outbox message was not found. id=" + messageId));
    }

}
