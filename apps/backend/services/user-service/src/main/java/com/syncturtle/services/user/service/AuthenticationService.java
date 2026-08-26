package com.syncturtle.services.user.service;

import java.util.UUID;

import com.syncturtle.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.dto.response.IssueTokenWithUserResponse;
import com.syncturtle.services.user.dto.response.MagicCodeResponse;
import com.syncturtle.services.user.dto.response.SignOutResponse;

public interface AuthenticationService {
    EmailCheckResponse emailCheck(String email);

    MagicCodeResponse generateMagicCode(String email);

    IssueTokenResponse emailPasswordSignIn(String email, String password, String nextPath);

    IssueTokenResponse emailPasswordSignUp(String email, String password, String nextPath);

    IssueTokenResponse magicCodeSignIn(String email, String code, String nextPath);

    IssueTokenResponse magicCodeSignUp(String email, String code, String nextPath);

    SignOutResponse signOut(String logoutContext, String presentedRefreshToken);

    IssueTokenWithUserResponse setPassword(UUID currentUserId, String sessionId, String password);
}
