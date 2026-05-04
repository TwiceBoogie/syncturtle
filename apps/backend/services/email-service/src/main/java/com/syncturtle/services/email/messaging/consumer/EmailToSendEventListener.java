package com.syncturtle.services.email.messaging.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.email.service.EmailEventInboxService;
import com.syncturtle.services.email.service.EmailInboxProcessor;
import com.syncturtle.services.email.service.EmailEventInboxService.AcquireResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailToSendEventListener {

    private final EmailEventInboxService emailEventInboxService;
    private final EmailInboxProcessor emailInboxProcessor;

    @KafkaListener(topics = KafkaTopics.EMAIL_EVENTS_V1, groupId = "${app.kafka.consumer-group}", containerFactory = "emailToSendKafkaListenerFactory")
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
