package com.syncturtle.services.instance.messaging.kafka.consumer;

import java.time.Clock;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.services.instance.messaging.kafka.mapper.UserEventMapper;
import com.syncturtle.services.instance.model.UserLite;
import com.syncturtle.services.instance.model.param.UserReplicaParam;
import com.syncturtle.services.instance.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUserEventConsumer {

    private final UserRepository repository;
    private final UserEventMapper mapper;
    private final Clock clock;

    @Transactional
    @KafkaListener(topics = KafkaTopics.USER_EVENTS_V1, groupId = "instance-svc-user-event-v1", containerFactory = "userKafkaListenerFactory")
    public void onUser(UserEvent event) {
        Assert.notNull(event, "user event is required");

        UserReplicaParam param = mapper.toParam(event);

        repository.findById(param.getId()).ifPresentOrElse(
                existing -> applyToExisting(existing, param),
                () -> insertNew(param));

    }

    private void applyToExisting(UserLite existing, UserReplicaParam param) {
        boolean changed = existing.applyReplicaParam(param, clock);

        if (!changed) {
            log.debug(
                    "Ignored stale or duplicate user replica. userId={} incomingVersion={} currentVersion={}",
                    param.getId(),
                    param.getSourceVersion(),
                    existing.getSourceVersion());
            return;
        }

        repository.save(existing);
    }

    private void insertNew(UserReplicaParam param) {
        UserLite user = UserLite.fromReplicaParam(param, clock);
        repository.save(user);
    }

}
