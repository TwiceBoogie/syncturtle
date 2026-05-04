package com.syncturtle.services.user.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthRuntimeConfig {
    private boolean signupEnabled;
    private boolean magicLinkEnabled;
    private boolean emailPasswordEnabled;
    private boolean smtpEnabled;

    private boolean googleEnabled;
    private boolean githubEnabled;
    private boolean gitlabEnabled;

    private long version;
}
