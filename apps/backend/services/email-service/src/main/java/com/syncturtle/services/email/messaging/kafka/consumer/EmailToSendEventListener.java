package com.syncturtle.services.email.messaging.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.email.service.EmailInboxService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailToSendEventListener {

    private final EmailInboxService service;

    @KafkaListener(topics = KafkaTopics.EMAIL_EVENTS_V1, groupId = "${app.kafka.consumer-group}", containerFactory = "emailToSendKafkaListenerFactory")
    public void onEmailToSend(EmailToSendEvent event) {
        service.receive(event);
    }

}
