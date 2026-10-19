package com.syncturtle.services.user.service;

import com.syncturtle.services.user.dto.response.IssueTokenResponse;

public interface OAuthService {
    String googleOAuthInitiate(String nextPath, String browserBindingHash);

    IssueTokenResponse googleOAuthCallback(String code, String state, String browserBindingHash);

    String githubOAuthInitiate(String nextPath, String browserBindingHash);

    IssueTokenResponse githubOAuthCallback(String code, String state, String browserBindingHash);

    String gitlabOAuthInitiate(String nextPath, String browserBindingHash);

    IssueTokenResponse gitlabOAuthCallback(String code, String state, String browserBindingHash);
}
