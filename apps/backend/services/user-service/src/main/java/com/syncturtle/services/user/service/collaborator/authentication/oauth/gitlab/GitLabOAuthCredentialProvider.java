package com.syncturtle.services.user.service.collaborator.authentication.oauth.gitlab;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.provider.AuthProvider;
import com.syncturtle.services.user.client.oauth.gitlab.GitLabOAuthClient;
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
public class GitLabOAuthCredentialProvider {

    private final GitLabOAuthClient gitLabOAuthClient;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CredentialAuthenticationWorkflow workflow;
    private final Clock clock;

    public String authorizationUrl(String state) {
        return gitLabOAuthClient.authorizationUrl(state);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OAuthAuthenticationReceipt authenticate(OAuthAuthenticationParam param) {
        Assert.notNull(param, "gitlab oauth authentication param is required");

        GitLabOAuthUser gitLabUser = gitLabOAuthClient.authenticate(param.getCode());

        boolean existingUser = userRepository.existsByEmailIgnoreCase(gitLabUser.getEmail());

        User user = workflow.completeLoginOrSignup(CredentialCompletionParam.builder()
                .provider(CredentialProviderType.GITLAB)
                .userData(CredentialUserDataParam.builder()
                        .email(gitLabUser.getEmail())
                        .firstName(gitLabUser.getFirstName())
                        .lastName(gitLabUser.getLastName())
                        .displayName(gitLabUser.getDisplayName())
                        .avatarUrl(gitLabUser.getAvatarUrl())
                        .passwordAutoset(true)
                        .rawPassword(null)
                        .build())
                .ipAddress(param.getIpAddress())
                .userAgent(param.getUserAgent())
                .build());

        upsertGitLabAccount(user, gitLabUser);

        return existingUser ? OAuthAuthenticationReceipt.existingUser(user)
                : OAuthAuthenticationReceipt.createdUser(user);
    }

    private Account upsertGitLabAccount(User user, GitLabOAuthUser gitLabUser) {
        Assert.notNull(user, "user is required");
        Assert.notNull(gitLabUser, "gitlab oauth user is required");

        AccountConnectionParam connection = AccountConnectionParam.builder()
                .provider(AuthProvider.GITLAB)
                .providerAccountId(gitLabUser.getProviderAccountId())
                .accessToken(gitLabUser.getAccessToken())
                .accessTokenExpiresAt(gitLabUser.getAccessTokenExpiredAt())
                .refreshToken(gitLabUser.getRefreshToken())
                .refreshTokenExpiresAt(gitLabUser.getRefreshTokenExpiredAt())
                .idToken(gitLabUser.getIdToken())
                .metadata(gitLabUser.getMetadata())
                .build();

        Account account = accountRepository
                .findByProviderAndProviderAccountId(connection.getProvider(), connection.getProviderAccountId())
                .orElseGet(() -> Account.create(AccountCreateParam.builder()
                        .user(user)
                        .provider(connection.getProvider())
                        .providerAccountId(connection.getProviderAccountId())
                        .connection(connection)
                        .build(), clock));

        requireAccountBelongsToUser(account, user);

        account.reconnect(connection, clock);

        return accountRepository.save(account);
    }

    private void requireAccountBelongsToUser(Account account, User user) {
        Assert.notNull(account, "account is required");
        Assert.notNull(user, "user is required");

        if (!account.belongsTo(user)) {
            throw AuthException.of(AuthErrorCode.GITLAB_OAUTH_PROVIDER_ERROR)
                    .with("error", "GitLab account is already linked to another user");
        }
    }

}
