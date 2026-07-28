package com.syncturtle.services.user.service.authentication.oauth.github;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.user.dto.response.GitHubEmailResponse;
import com.syncturtle.services.user.dto.response.GitHubTokenResponse;
import com.syncturtle.services.user.dto.response.GitHubUserInfoResponse;
import com.syncturtle.services.user.service.runtime.UserAuthRuntimeSecretConfig;
import com.syncturtle.services.user.service.runtime.UserAuthRuntimeSecretResolver;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@Component
@RequiredArgsConstructor
public class GitHubOAuthClient {

    private static final String AUTHORIZATION_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USERINFO_URL = "https://api.github.com/user";
    private static final String EMAILS_URL = "https://api.github.com/user/emails";
    private static final String ORG_MEMBERSHIP_URL = "https://api.github.com/orgs";

    private static final String BASE_SCOPE = "read:user user:email";
    private static final String ORGANIZATION_SCOPE = "read:org";
    private static final String REDIRECT_URL = "/auth/github/callback";

    private final RestClient restClient;
    private final UserAuthRuntimeSecretResolver configSecretResolver;
    private final PublicUrlResolver hostResolver;
    private final Clock clock;

    public String authorizationUrl(String state) {
        Assert.hasText(state, "github oauth state is required");

        GitHubOAuthClientCredentials credentials = requireGitHubClientCredentials();

        return UriComponentsBuilder.fromUriString(AUTHORIZATION_URL)
                .queryParam("client_id", credentials.getClientId())
                .queryParam("redirect_uri", hostResolver.api(REDIRECT_URL))
                .queryParam("scope", resolveScope(credentials))
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    public GitHubOAuthUser authenticate(String code) {
        Assert.hasText(code, "github oauth code is required");

        GitHubOAuthClientCredentials credentials = requireGitHubClientCredentials();

        GitHubTokenResponse tokenResponse = exchangeCodeForToken(code, credentials);
        GitHubUserInfoResponse userInfoResponse = fetchUserInfo(code);

        requireOrganizationMembershipIfConfigured(credentials, tokenResponse, userInfoResponse);

        String email = fetchPrimaryEmail(tokenResponse.getAccessToken());

        return GitHubOAuthUser.builder()
                .providerAccountId(String.valueOf(userInfoResponse.getId()))
                .email(email)
                .avatarUrl(userInfoResponse.getAvatarUrl())
                .firstName(userInfoResponse.getName())
                .lastName(null)
                .displayName(resolveDisplayName(userInfoResponse, email))
                .accessToken(tokenResponse.getAccessToken())
                .accessTokenExpiredAt(resolveAccessTokenExpiredAt(tokenResponse))
                .refreshToken(tokenResponse.getRefreshToken())
                .refreshTokenExpiredAt(null)
                .idToken(tokenResponse.getIdToken())
                .metadata(metadataFrom(tokenResponse, userInfoResponse, email, credentials))
                .build();
    }

    private GitHubTokenResponse exchangeCodeForToken(String code, GitHubOAuthClientCredentials credentials) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("client_id", credentials.getClientId());
        body.add("client_secret", credentials.getClientSecret());
        body.add("code", code);
        body.add("redirect_uri", hostResolver.api(REDIRECT_URL));

        try {
            GitHubTokenResponse response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GitHubTokenResponse.class);

            if (response == null || !StringUtils.hasText(response.getAccessToken())) {
                throw AuthException.of(AuthErrorCode.GITHUB_OAUTH_PROVIDER_ERROR);
            }

            return response;
        } catch (RestClientException exception) {
            throw AuthException.of(AuthErrorCode.GITHUB_OAUTH_PROVIDER_ERROR);
        }
    }

    private GitHubUserInfoResponse fetchUserInfo(String accessToken) {
        Assert.hasText(accessToken, "github accessToken is required");

        try {
            GitHubUserInfoResponse response = restClient.get()
                    .uri(USERINFO_URL)
                    .headers(headers -> {
                        headers.setBearerAuth(accessToken);
                        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
                    })
                    .retrieve()
                    .body(GitHubUserInfoResponse.class);

            if (response == null || response.getId() == null || !StringUtils.hasText(response.getLogin())) {
                throw AuthException.of(AuthErrorCode.GITHUB_OAUTH_PROVIDER_ERROR);
            }

            return response;
        } catch (RestClientException exception) {
            throw AuthException.of(AuthErrorCode.GITHUB_OAUTH_PROVIDER_ERROR);
        }
    }

    private String fetchPrimaryEmail(String accessToken) {
        Assert.hasText(accessToken, "github accessToken is required");

        try {
            GitHubEmailResponse[] response = restClient.get()
                    .uri(EMAILS_URL)
                    .headers(headers -> {
                        headers.setBearerAuth(accessToken);
                        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
                    })
                    .retrieve()
                    .body(GitHubEmailResponse[].class);

            if (response == null || response.length == 0) {
                throw AuthException.of(AuthErrorCode.GITHUB_OAUTH_PROVIDER_ERROR);
            }

            return Arrays.stream(response)
                    .filter(GitHubEmailResponse::isPrimary)
                    .map(GitHubEmailResponse::getEmail)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElseThrow(() -> AuthException.of(AuthErrorCode.GITHUB_OAUTH_PROVIDER_ERROR));
        } catch (RestClientException exception) {
            throw AuthException.of(AuthErrorCode.GITHUB_OAUTH_PROVIDER_ERROR);
        }
    }

    private void requireOrganizationMembershipIfConfigured(GitHubOAuthClientCredentials credentials,
            GitHubTokenResponse tokenResponse, GitHubUserInfoResponse userInfoResponse) {
        if (!StringUtils.hasText(credentials.getOrganizationId())) {
            return;
        }

        try {
            restClient.get()
                    .uri(ORG_MEMBERSHIP_URL + "{organizationId}/memberships/{username}",
                            credentials.getOrganizationId(), userInfoResponse.getLogin())
                    .headers(headers -> headers.setBearerAuth(tokenResponse.getAccessToken()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw AuthException.of(AuthErrorCode.GITHUB_OAUTH_PROVIDER_ERROR);
        }
    }

    private GitHubOAuthClientCredentials requireGitHubClientCredentials() {
        UserAuthRuntimeSecretConfig configurations = configSecretResolver.get();

        Assert.notNull(configurations, "user auth runtime secret config is required");

        String clientId = configurations.getGithubClientId();
        String clientSecret = configurations.getGithubClientSecret();
        String organizationId = configurations.getGithubAppName();

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw AuthException.of(AuthErrorCode.GITHUB_NOT_CONFIGURED);
        }

        return GitHubOAuthClientCredentials.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .organizationId(organizationId)
                .build();
    }

    private String resolveScope(GitHubOAuthClientCredentials credentials) {
        if (StringUtils.hasText(credentials.getOrganizationId())) {
            return BASE_SCOPE + " " + ORGANIZATION_SCOPE;
        }

        return BASE_SCOPE;
    }

    private Instant resolveAccessTokenExpiredAt(GitHubTokenResponse response) {
        if (response.getExpiresIn() == null) {
            return null;
        }

        return clock.instant().plusSeconds(response.getExpiresIn());
    }

    private String resolveDisplayName(GitHubUserInfoResponse response, String email) {
        if (StringUtils.hasText(response.getName())) {
            return response.getName().trim();
        }

        if (StringUtils.hasText(response.getLogin())) {
            return response.getLogin().trim();
        }

        return email;
    }

    private JsonNode metadataFrom(GitHubTokenResponse tokenResponse, GitHubUserInfoResponse userInfoResponse,
            String email, GitHubOAuthClientCredentials credentials) {
        ObjectNode metadata = JsonNodeFactory.instance.objectNode();

        metadata.put("email", email);
        metadata.put("login", userInfoResponse.getLogin());
        metadata.put("name", userInfoResponse.getName());
        metadata.put("avatarUrl", userInfoResponse.getAvatarUrl());
        metadata.put("htmlUrl", userInfoResponse.getHtmlUrl());
        metadata.put("scope", tokenResponse.getScope());
        metadata.put("tokenType", tokenResponse.getTokenType());

        if (StringUtils.hasText(credentials.getOrganizationId())) {
            metadata.put("organizationId", credentials.getOrganizationId());
        }

        return metadata;
    }

}
