package com.syncturtle.platform.services.email.messaging.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.syncturtle.common.core.constants.KafkaTopicConstants;
import com.syncturtle.common.core.events.EmailToSendEvent;
import com.syncturtle.platform.services.email.service.EmailEventInboxService;
import com.syncturtle.platform.services.email.service.EmailInboxProcessor;
import com.syncturtle.platform.services.email.service.EmailEventInboxService.AcquireResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailToSendEventListener {

    private final EmailEventInboxService emailEventInboxService;
    private final EmailInboxProcessor emailInboxProcessor;

    @KafkaListener(topics = KafkaTopicConstants.EMAIL_EVENTS_V1, groupId = "${app.kafka.consumer-group}")
    public void onEmailToSend(EmailToSendEvent event) {
        AcquireResult acquire = emailEventInboxService.tryAcquire(event);

        switch (acquire.decision()) {
            case ALREADY_SENT -> {
                log.info("Skipping duplicate email event already marked SENT. eventId={}", event.getEventId());
                return;
            }
            case IN_PROGRESS -> {
                log.info("Skipping email event already being processed. eventId={}", event.getEventId());
                return;
            }
            case RETRY_SCHEDULED -> {
                log.info("Skipping duplicate email event already scheduled for DB retry. eventId={}",
                        event.getEventId());
                return;
            }
            case PERMANENT_FAILURE -> {
                log.warn("Skipping duplicate email event already marked permanently failed. eventId={}",
                        event.getEventId());
                return;
            }
            case ACQUIRED -> emailInboxProcessor.process(acquire.row());
        }
    }

}
