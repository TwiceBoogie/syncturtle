package com.syncturtle.services.user.service.collaborator.authentication.oauth.google;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.provider.AuthProvider;
import com.syncturtle.services.user.client.oauth.google.GoogleOAuthClient;
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
public class GoogleOAuthCredentialProvider {

    private final GoogleOAuthClient googleOAuthClient;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CredentialAuthenticationWorkflow workflow;
    private final Clock clock;

    public String authorizationUrl(String state) {
        return googleOAuthClient.authorizationUrl(state);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OAuthAuthenticationReceipt authenticate(OAuthAuthenticationParam param) {
        Assert.notNull(param, "google oauth authentication param is required");

        GoogleOAuthUser googleUser = googleOAuthClient.authenticate(param.getCode());

        boolean existingUser = userRepository.existsByEmailIgnoreCase(googleUser.getEmail());

        User user = workflow.completeLoginOrSignup(CredentialCompletionParam.builder()
                .provider(CredentialProviderType.GOOGLE)
                .userData(CredentialUserDataParam.builder()
                        .email(googleUser.getEmail())
                        .firstName(googleUser.getFirstName())
                        .lastName(googleUser.getLastName())
                        .displayName(googleUser.getDisplayName())
                        .avatarUrl(googleUser.getAvatarUrl())
                        .passwordAutoset(true)
                        .rawPassword(null)
                        .build())
                .ipAddress(param.getIpAddress())
                .userAgent(param.getUserAgent())
                .build());

        upsertGoogleAccount(user, googleUser);

        return existingUser ? OAuthAuthenticationReceipt.existingUser(user)
                : OAuthAuthenticationReceipt.createdUser(user);
    }

    private Account upsertGoogleAccount(User user, GoogleOAuthUser googleUser) {
        Assert.notNull(user, "user is required");
        Assert.notNull(googleUser, "google oauth user is required");

        AccountConnectionParam connection = AccountConnectionParam.builder()
                .provider(AuthProvider.GOOGLE)
                .providerAccountId(googleUser.getProviderAccountId())
                .accessToken(googleUser.getAccessToken())
                .accessTokenExpiresAt(googleUser.getAccessTokenExpiredAt())
                .refreshToken(googleUser.getRefreshToken())
                .refreshTokenExpiresAt(googleUser.getRefreshTokenExpiredAt())
                .idToken(googleUser.getIdToken())
                .metadata(googleUser.getMetadata())
                .build();

        Account account = accountRepository
                .findByProviderAndProviderAccountId(connection.getProvider(), connection.getProviderAccountId())
                .orElseGet(() -> Account.create(AccountCreateParam.builder()
                        .user(user)
                        .provider(connection.getProvider())
                        .providerAccountId(connection.getProviderAccountId()).connection(connection)
                        .build(), clock));
        requireAccountBelongsToUser(account, user);

        account.reconnect(connection, clock);

        return accountRepository.save(account);
    }

    private void requireAccountBelongsToUser(Account account, User user) {
        Assert.notNull(account, "account is required");
        Assert.notNull(user, "user is required");

        if (!account.belongsTo(user)) {
            throw AuthException.of(AuthErrorCode.GOOGLE_OAUTH_PROVIDER_ERROR).with("error",
                    "Google account is already linked to another user");
        }
    }

}
