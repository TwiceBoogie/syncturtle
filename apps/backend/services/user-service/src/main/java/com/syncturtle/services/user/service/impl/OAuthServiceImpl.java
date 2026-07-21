package com.syncturtle.services.user.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.provider.AuthProvider;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.model.Instance;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.InstanceRepository;
import com.syncturtle.services.user.service.OAuthService;
import com.syncturtle.services.user.service.authentication.oauth.OAuthState;
import com.syncturtle.services.user.service.authentication.oauth.OAuthStateStore;
import com.syncturtle.services.user.service.authentication.oauth.github.GitHubOAuthCredentialProvider;
import com.syncturtle.services.user.service.authentication.oauth.gitlab.GitLabOAuthCredentialProvider;
import com.syncturtle.services.user.service.authentication.oauth.OAuthAuthenticationReceipt;
import com.syncturtle.services.user.service.authentication.oauth.OAuthAuthenticationSpec;
import com.syncturtle.services.user.service.authentication.oauth.google.GoogleOAuthCredentialProvider;
import com.syncturtle.services.user.service.authentication.redirect.AuthenticationRedirector;
import com.syncturtle.services.user.service.runtime.UserAuthRuntimeConfigResolver;
import com.syncturtle.services.user.service.runtime.UserAuthRuntimeSnapshot;
import com.syncturtle.services.user.service.session.AuthenticatedSessionIssueSpec;
import com.syncturtle.services.user.service.session.AuthenticatedSessionIssuer;
import com.syncturtle.services.user.service.session.AuthenticatedSessionReceipt;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final OAuthStateStore oAuthStateStore;
    private final GoogleOAuthCredentialProvider googleProvider;
    private final GitHubOAuthCredentialProvider githubProvider;
    private final GitLabOAuthCredentialProvider gitlabProvider;
    private final UserAuthRuntimeConfigResolver configFlagResolver;
    private final InstanceRepository instanceRepository;
    private final AuthenticationRedirector authenticationRedirector;
    private final AuthenticatedSessionIssuer authenticatedSessionIssuer;
    private final RequestClientContext clientContext;

    @Override
    @Transactional(readOnly = true)
    public String googleOAuthInitiate(String nextPath) {
        try {
            requireInstanceSetup();
            requireGoogleOAuthEnabled();

            String state = UUID.randomUUID().toString().replace("-", "");

            oAuthStateStore.save(OAuthState.builder()
                    .state(state)
                    .nextPath(nextPath)
                    .build(), AuthProvider.GOOGLE);

            return googleProvider.authorizationUrl(state);
        } catch (AuthException exception) {
            return authenticationRedirector.signInFailure(exception, nextPath).getLocation();
        }
    }

    @Override
    @Transactional
    public IssueTokenResponse googleOAuthCallback(String code, String state) {
        OAuthState oAuthState = null;

        try {
            Instance instance = requireInstanceSetup();

            requireOAuthCallbackInput(code, state, AuthProvider.GOOGLE);

            oAuthState = oAuthStateStore.consume(state, AuthProvider.GOOGLE)
                    .orElseThrow(() -> AuthException.of(AuthErrorCode.GOOGLE_OAUTH_PROVIDER_ERROR));

            OAuthAuthenticationReceipt result = googleProvider
                    .authenticate(OAuthAuthenticationSpec.builder()
                            .code(code)
                            .ipAddress(clientContext.getClientIp())
                            .userAgent(clientContext.getUserAgent())
                            .build());

            return issueTokenResponse(result.getUser(), instance.getId(), oAuthState.getNextPath());
        } catch (AuthException exception) {
            String nextPath = oAuthState == null ? null : oAuthState.getNextPath();
            return authenticationRedirector.signInFailure(exception, nextPath);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String githubOAuthInitiate(String nextPath) {
        try {
            requireInstanceSetup();
            requireGitHubOAuthEnabled();

            String state = UUID.randomUUID().toString().replace("-", "");

            oAuthStateStore.save(OAuthState.builder()
                    .state(state)
                    .nextPath(nextPath).build(), AuthProvider.GITHUB);

            return githubProvider.authorizationUrl(state);
        } catch (AuthException exception) {
            return authenticationRedirector.signInFailure(exception, nextPath).getLocation();
        }
    }

    @Override
    @Transactional
    public IssueTokenResponse githubOAuthCallback(String code, String state) {
        OAuthState oAuthState = null;

        try {
            Instance instance = requireInstanceSetup();

            requireOAuthCallbackInput(code, state, AuthProvider.GITHUB);

            oAuthState = oAuthStateStore.consume(state, AuthProvider.GITHUB)
                    .orElseThrow(() -> AuthException.of(AuthErrorCode.GITHUB_OAUTH_PROVIDER_ERROR));

            OAuthAuthenticationReceipt result = githubProvider
                    .authenticate(OAuthAuthenticationSpec.builder()
                            .code(code)
                            .ipAddress(clientContext.getClientIp())
                            .userAgent(clientContext.getUserAgent())
                            .build());

            return issueTokenResponse(result.getUser(), instance.getId(), oAuthState.getNextPath());
        } catch (AuthException exception) {
            String nextPath = oAuthState == null ? null : oAuthState.getNextPath();
            return authenticationRedirector.signInFailure(exception, nextPath);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String gitlabOAuthInitiate(String nextPath) {
        try {
            requireInstanceSetup();
            requireGitLabOAuthEnabled();

            String state = UUID.randomUUID().toString().replace("-", "");

            oAuthStateStore.save(OAuthState.builder()
                    .state(state)
                    .nextPath(nextPath).build(), AuthProvider.GITLAB);

            return gitlabProvider.authorizationUrl(state);
        } catch (AuthException exception) {
            return authenticationRedirector.signInFailure(exception, nextPath).getLocation();
        }
    }

    @Override
    @Transactional
    public IssueTokenResponse gitlabOAuthCallback(String code, String state) {
        OAuthState oAuthState = null;

        try {
            Instance instance = requireInstanceSetup();

            requireOAuthCallbackInput(code, state, AuthProvider.GITLAB);

            oAuthState = oAuthStateStore.consume(state, AuthProvider.GITLAB)
                    .orElseThrow(() -> AuthException.of(AuthErrorCode.GITLAB_OAUTH_PROVIDER_ERROR));

            OAuthAuthenticationReceipt result = gitlabProvider
                    .authenticate(OAuthAuthenticationSpec.builder()
                            .code(code)
                            .ipAddress(clientContext.getClientIp())
                            .userAgent(clientContext.getUserAgent())
                            .build());

            return issueTokenResponse(result.getUser(), instance.getId(), oAuthState.getNextPath());
        } catch (AuthException exception) {
            String nextPath = oAuthState == null ? null : oAuthState.getNextPath();
            return authenticationRedirector.signInFailure(exception, nextPath);
        }
    }

    private Instance requireInstanceSetup() {
        Instance instance = instanceRepository.findFirstByOrderByCreatedAtAsc().orElse(null);

        if (instance == null || !instance.isSetupDone()) {
            throw AuthException.of(AuthErrorCode.INSTANCE_NOT_CONFIGURED);
        }

        return instance;
    }

    private void requireGoogleOAuthEnabled() {
        UserAuthRuntimeSnapshot configurations = configFlagResolver.getInstanceConfigurations();
        Assert.notNull(configurations, "user auth runtime config is required");

        if (!configurations.isGoogleEnabled()) {
            throw AuthException.of(AuthErrorCode.GOOGLE_NOT_CONFIGURED);
        }
    }

    private void requireGitLabOAuthEnabled() {
        UserAuthRuntimeSnapshot configurations = configFlagResolver.getInstanceConfigurations();
        Assert.notNull(configurations, "user auth runtime config is required");

        if (!configurations.isGitlabEnabled()) {
            throw AuthException.of(AuthErrorCode.GITLAB_NOT_CONFIGURED);
        }
    }

    private void requireGitHubOAuthEnabled() {
        UserAuthRuntimeSnapshot configurations = configFlagResolver.getInstanceConfigurations();
        Assert.notNull(configurations, "user auth runtime config is required");

        if (!configurations.isGithubEnabled()) {
            throw AuthException.of(AuthErrorCode.GITHUB_NOT_CONFIGURED);
        }
    }

    private void requireOAuthCallbackInput(String code, String state, AuthProvider provider) {
        if (!StringUtils.hasText(code) || !StringUtils.hasText(state)) {
            switch (provider) {
                case GOOGLE:
                    throw AuthException.of(AuthErrorCode.GOOGLE_OAUTH_PROVIDER_ERROR);
                case GITHUB:
                    throw AuthException.of(AuthErrorCode.GITHUB_OAUTH_PROVIDER_ERROR);
                case GITLAB:
                    throw AuthException.of(AuthErrorCode.GITLAB_OAUTH_PROVIDER_ERROR);
                default:
                    throw AuthException.of(AuthErrorCode.OAUTH_NOT_CONFIGURED);
            }
        }
    }

    private IssueTokenResponse issueTokenResponse(User user, UUID instanceId, String nextPath) {
        Assert.notNull(user, "user is required");
        Assert.notNull(instanceId, "instanceId is required");

        AuthenticatedSessionReceipt session = authenticatedSessionIssuer.issueNewSession(
                AuthenticatedSessionIssueSpec.builder()
                        .user(user)
                        .instanceId(instanceId)
                        .ipAddress(clientContext.getClientIp())
                        .userAgent(clientContext.getUserAgent())
                        .build());

        return IssueTokenResponse.issued(session, authenticationRedirector.successLocation(nextPath));
    }

}
