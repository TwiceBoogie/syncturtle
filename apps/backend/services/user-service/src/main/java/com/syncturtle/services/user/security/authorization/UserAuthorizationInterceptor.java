package com.syncturtle.services.user.security.authorization;

import com.syncturtle.common.web.context.RequestUserContext;

public final class UserAuthorizationInterceptor extends AbstractAuthorizationInterceptor {

    public UserAuthorizationInterceptor(RequestUserContext requestUserContext, Boolean requireAuthenticationByDefault) {
        super(requestUserContext, requireAuthenticationByDefault);
    }

}
