package com.syncturtle.common.contracts.auth.config;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class UserAuthRuntimeConfigResponse {
    boolean signupEnabled;
    boolean magicLinkEnabled;
    boolean emailPasswordEnabled;
    boolean smtpEnabled;

    boolean googleEnabled;
    boolean githubEnabled;
    boolean gitlabEnabled;

    Long version;
}
