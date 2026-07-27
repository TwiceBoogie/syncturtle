package com.syncturtle.services.instance.messaging.outbox;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.services.instance.model.OutboxMessage;
import com.syncturtle.services.instance.repository.OutboxMessageRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxMessageClaimer {

    private final OutboxMessageRepository repository;
    private final Clock clock;

    @Transactional
    public List<OutboxEnvelope> claimDueMessages(String workerId, int batchSize) {
        Assert.hasText(workerId, "workerId is required");
        Assert.isTrue(batchSize > 0, "batchSize must be greater than 0");

        Instant now = Instant.now(clock);

        List<OutboxMessage> messages = repository.findDueForPublishing(now, batchSize);

        for (OutboxMessage message : messages) {
            message.markPublishing(workerId, clock);
        }

        return messages.stream()
                .map(OutboxMessage::toEnvelope)
                .toList();
    }

    @Transactional
    public int releaseExpiredPublishingLocks(Duration lockTimeout, int batchSize) {
        Assert.notNull(lockTimeout, "lockTimeout is required");
        Assert.isTrue(!lockTimeout.isNegative(), "lockTimeout must not be negative");
        Assert.isTrue(batchSize > 0, "batchSize must be greater than 0");

        Instant lockedBefore = Instant.now(clock).minus(lockTimeout);

        List<OutboxMessage> messages = repository.findExpiredPublishingLocks(lockedBefore, batchSize);

        for (OutboxMessage message : messages) {
            message.releaseExpiredLock(lockTimeout, clock);
        }

        return messages.size();
    }

}
