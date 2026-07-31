package com.syncturtle.services.user.service;

import com.syncturtle.services.user.dto.response.IssueTokenResponse;

public interface OAuthService {
    String googleOAuthInitiate(String nextPath);

    IssueTokenResponse googleOAuthCallback(String code, String state);

    String githubOAuthInitiate(String nextPath);

    IssueTokenResponse githubOAuthCallback(String code, String state);

    String gitlabOAuthInitiate(String nextPath);

    IssueTokenResponse gitlabOAuthCallback(String code, String state);
}
