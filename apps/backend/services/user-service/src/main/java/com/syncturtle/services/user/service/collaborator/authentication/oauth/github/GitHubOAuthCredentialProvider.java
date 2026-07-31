package com.syncturtle.services.user.service.collaborator.authentication.oauth.github;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.provider.AuthProvider;
import com.syncturtle.services.user.client.oauth.github.GitHubOAuthClient;
import com.syncturtle.services.user.model.Account;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.model.param.AccountConnectionParam;
import com.syncturtle.services.user.model.param.AccountCreateParam;
import com.syncturtle.services.user.repository.AccountRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.collaborator.authentication.oauth.OAuthAuthenticationReceipt;
import com.syncturtle.services.user.service.collaborator.authentication.workflow.CredentialAuthenticationWorkflow;
import com.syncturtle.services.user.service.param.CredentialCompletionParam;
import com.syncturtle.services.user.service.param.CredentialUserDataParam;
import com.syncturtle.services.user.service.param.OAuthAuthenticationParam;
import com.syncturtle.services.user.type.CredentialProviderType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GitHubOAuthCredentialProvider {

    private final GitHubOAuthClient gitHubOAuthClient;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CredentialAuthenticationWorkflow workflow;
    private final Clock clock;

    public String authorizationUrl(String state) {
        return gitHubOAuthClient.authorizationUrl(state);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OAuthAuthenticationReceipt authenticate(OAuthAuthenticationParam param) {
        Assert.notNull(param, "github oauth authentication param is required");

        GitHubOAuthUser gitHubUser = gitHubOAuthClient.authenticate(param.getCode());

        boolean existingUser = userRepository.existsByEmailIgnoreCase(gitHubUser.getEmail());

        User user = workflow.completeLoginOrSignup(CredentialCompletionParam.builder()
                .provider(CredentialProviderType.GITHUB)
                .userData(CredentialUserDataParam.builder()
                        .email(gitHubUser.getEmail())
                        .firstName(gitHubUser.getFirstName())
                        .lastName(gitHubUser.getLastName())
                        .displayName(gitHubUser.getDisplayName())
                        .avatarUrl(gitHubUser.getAvatarUrl())
                        .passwordAutoset(true)
                        .rawPassword(null)
                        .build())
                .ipAddress(param.getIpAddress())
                .userAgent(param.getUserAgent())
                .build());

        upsertGitHubAccount(user, gitHubUser);

        return existingUser ? OAuthAuthenticationReceipt.existingUser(user)
                : OAuthAuthenticationReceipt.createdUser(user);
    }

    private Account upsertGitHubAccount(User user, GitHubOAuthUser gitHubUser) {
        Assert.notNull(user, "user is required");
        Assert.notNull(gitHubUser, "github oauth user is required");

        AccountConnectionParam connection = AccountConnectionParam.builder()
                .provider(AuthProvider.GITHUB)
                .providerAccountId(gitHubUser.getProviderAccountId())
                .accessToken(gitHubUser.getAccessToken())
                .accessTokenExpiresAt(gitHubUser.getAccessTokenExpiredAt())
                .refreshToken(gitHubUser.getRefreshToken())
                .refreshTokenExpiresAt(gitHubUser.getRefreshTokenExpiredAt())
                .idToken(gitHubUser.getIdToken())
                .metadata(gitHubUser.getMetadata())
                .build();

        Account account = accountRepository
                .findByProviderAndProviderAccountId(connection.getProvider(), connection.getProviderAccountId())
                .orElseGet(() -> Account.create(AccountCreateParam.builder()
                        .user(user)
                        .provider(connection.getProvider())
                        .providerAccountId(connection.getProviderAccountId())
                        .build(), clock));

        requireAccountBelongsToUser(account, user);

        account.reconnect(connection, clock);

        return accountRepository.save(account);
    }

    private void requireAccountBelongsToUser(Account account, User user) {
        Assert.notNull(account, "account is required");
        Assert.notNull(user, "user is required");

        if (!account.belongsTo(user)) {
            throw AuthException.of(AuthErrorCode.GITHUB_OAUTH_PROVIDER_ERROR)
                    .with("error", "GitHub account is already linked to another user");
        }
    }

}
