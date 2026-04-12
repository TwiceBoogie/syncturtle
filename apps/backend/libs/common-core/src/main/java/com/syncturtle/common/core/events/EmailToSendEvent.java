package com.syncturtle.common.core.events;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.syncturtle.common.core.enums.EmailTemplateType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailToSendEvent {
    private String eventId;
    private Instant occurredAt;

    private String correlationId;

    private EmailTemplateType templateType;
    private String subject;
    List<String> to;
    Map<String, Object> model;
}
