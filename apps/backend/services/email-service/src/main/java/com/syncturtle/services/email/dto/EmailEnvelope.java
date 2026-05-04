package com.syncturtle.services.email.dto;

import java.util.List;
import java.util.Map;

import com.syncturtle.common.contracts.email.template.EmailTemplateType;

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
