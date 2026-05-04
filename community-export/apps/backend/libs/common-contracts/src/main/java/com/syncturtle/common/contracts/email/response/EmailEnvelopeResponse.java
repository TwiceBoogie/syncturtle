package com.syncturtle.common.contracts.email.response;

import java.util.List;
import java.util.Map;

import com.syncturtle.common.contracts.email.template.EmailTemplateType;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class EmailEnvelopeResponse {
    EmailTemplateType templateType;
    String subject;
    List<String> to;
    Map<String, Object> model;
    String correlationId;
}
