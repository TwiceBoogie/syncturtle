package com.syncturtle.common.contracts.email.event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.syncturtle.common.contracts.email.template.EmailTemplateType;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class EmailToSendEvent {
    String eventId;
    Instant occurredAt;
    String correlationId;
    EmailTemplateType templateType;
    String subject;
    @Builder.Default
    List<String> to = new ArrayList<>();
    @Builder.Default
    Map<String, Object> model = new HashMap<>();
}
