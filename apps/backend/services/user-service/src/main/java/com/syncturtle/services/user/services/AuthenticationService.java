package com.syncturtle.services.user.services;

import com.syncturtle.services.user.dto.internal.RefreshExchangeResult;
import com.syncturtle.services.user.dto.request.SignInRequest;
import com.syncturtle.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;

public interface AuthenticationService {
    EmailCheckResponse emailCheck(String email);

    IssueTokenResponse signin(SignInRequest request);

    RefreshExchangeResult refreshSession(String presentedRefreshToken);

    String signOut(String logoutContext, String sessionId);
}
