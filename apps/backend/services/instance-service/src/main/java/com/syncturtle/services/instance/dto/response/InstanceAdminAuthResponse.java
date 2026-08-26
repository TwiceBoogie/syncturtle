package com.syncturtle.services.instance.dto.response;

import com.syncturtle.common.contracts.auth.session.AdminSessionHandoffResponse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InstanceAdminAuthResponse {
    String redirection;
    AdminSessionHandoffResponse handoff;
}
