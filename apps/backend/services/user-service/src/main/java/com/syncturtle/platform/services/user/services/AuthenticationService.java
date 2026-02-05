package com.syncturtle.platform.services.user.services;

import com.syncturtle.platform.services.user.dto.response.EmailCheckResponse;

public interface AuthenticationService {
    EmailCheckResponse emailCheck(String email);

    String signOut(String logoutContext);
}
