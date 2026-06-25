package com.syncturtle.services.user.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserMeSettingsResponse {
    UUID id;
    String email;
    UserMeSettingsWorkspaceResponse workspace;
}
