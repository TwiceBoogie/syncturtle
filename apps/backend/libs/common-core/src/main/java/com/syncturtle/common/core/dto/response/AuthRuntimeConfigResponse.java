package com.syncturtle.common.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRuntimeConfigResponse {
    private boolean signupEnabled;
    private boolean magicLinkEnabled;
    private boolean emailPasswordEnabled;
    private boolean smtpEnabled;
    private long scopeVersion;
    private long globalVersion;
}
