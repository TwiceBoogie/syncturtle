package com.syncturtle.platform.services.email.dto;

import java.util.List;
import java.util.Map;

import com.syncturtle.common.core.enums.EmailTemplateType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EmailEnvelope {
    EmailTemplateType templateType;
    String subject;
    List<String> to;
    Map<String, Object> model;
    String correlationId;
}
