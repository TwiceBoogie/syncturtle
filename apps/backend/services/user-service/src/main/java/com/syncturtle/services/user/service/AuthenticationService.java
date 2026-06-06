package com.syncturtle.services.user.service;

import java.util.UUID;

import com.syncturtle.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;

public interface AuthenticationService {
    EmailCheckResponse emailCheck(String email);

    IssueTokenResponse emailPasswordSignIn(String email, String password, String nextPath);

    IssueTokenResponse emailPasswordSignUp(String email, String password, String nextPath);

    IssueTokenResponse magicCodeSignIn(String email, String code, String nextPath);

    IssueTokenResponse magicCodeSignUp(String email, String code, String nextPath);

    String signOut(UUID currentUserId, String logoutContext, String sessionId);
}
