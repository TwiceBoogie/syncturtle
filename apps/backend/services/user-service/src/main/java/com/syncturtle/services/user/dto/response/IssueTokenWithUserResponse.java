package com.syncturtle.services.user.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IssueTokenWithUserResponse {
    IssueTokenResponse tokens;
    UserMeResponse user;
}
