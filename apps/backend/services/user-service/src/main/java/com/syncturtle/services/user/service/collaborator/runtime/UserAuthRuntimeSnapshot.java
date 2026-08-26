package com.syncturtle.services.user.service.collaborator.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthRuntimeSnapshot {
    private boolean signupEnabled;
    private boolean magicLinkEnabled;
    private boolean emailPasswordEnabled;
    private boolean smtpEnabled;

    private boolean googleEnabled;
    private boolean githubEnabled;
    private boolean gitlabEnabled;

    private long version;
}
