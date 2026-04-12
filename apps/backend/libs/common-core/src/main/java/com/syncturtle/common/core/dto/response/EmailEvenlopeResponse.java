package com.syncturtle.common.core.dto.response;

import java.util.List;
import java.util.Map;

import com.syncturtle.common.core.enums.EmailTemplateType;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class EmailEvenlopeResponse {
    EmailTemplateType templateType;
    String subject;
    List<String> to;
    Map<String, Object> model;
    String correlationId;
}
