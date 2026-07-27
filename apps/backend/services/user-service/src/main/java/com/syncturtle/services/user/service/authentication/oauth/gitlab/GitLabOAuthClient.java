package com.syncturtle.services.user.service.authentication.oauth.gitlab;

import java.time.Clock;
import java.time.Instant;

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
import com.syncturtle.services.user.dto.response.GitLabTokenResponse;
import com.syncturtle.services.user.dto.response.GitLabUserInfoResponse;
import com.syncturtle.services.user.service.runtime.UserAuthRuntimeSecretConfig;
import com.syncturtle.services.user.service.runtime.UserAuthRuntimeSecretResolver;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@Component
@RequiredArgsConstructor
public class GitLabOAuthClient {

    private static final String DEFAULT_GITLAB_HOST = "https://gitlab.com";
    private static final String SCOPE = "read_user";
    private static final String REDIRECT_URL = "/auth/gitlab/callback";

    private final RestClient restClient;
    private final UserAuthRuntimeSecretResolver configSecretResolver;
    private final PublicUrlResolver hostResolver;
    private final Clock clock;

    public String authorizationUrl(String state) {
        Assert.hasText(state, "gitlab oauth state is required");

        GitLabOAuthClientCredentials credentials = requireGitLabClientCredentials();

        return UriComponentsBuilder.fromUriString(credentials.getHost() + "/oauth/authorize")
                .queryParam("client_id", credentials.getClientId())
                .queryParam("redirect_uri", hostResolver.api(REDIRECT_URL))
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    public GitLabOAuthUser authenticate(String code) {
        Assert.hasText(code, "gitlab oauth code is required");

        GitLabOAuthClientCredentials credentials = requireGitLabClientCredentials();

        GitLabTokenResponse tokenResponse = exchangeCodeForToken(code, credentials);
        GitLabUserInfoResponse userInfoResponse = fetchUserInfo(tokenResponse.getAccessToken(), credentials);

        return GitLabOAuthUser.builder()
                .providerAccountId(String.valueOf(userInfoResponse.getId()))
                .email(userInfoResponse.getEmail())
                .avatarUrl(userInfoResponse.getAvatarUrl())
                .firstName(userInfoResponse.getName())
                .lastName(null)
                .displayName(resolveDisplayName(userInfoResponse))
                .accessToken(tokenResponse.getAccessToken())
                .accessTokenExpiredAt(resolveAccessTokenExpiredAt(tokenResponse))
                .refreshToken(tokenResponse.getRefreshToken())
                .refreshTokenExpiredAt(null)
                .idToken(tokenResponse.getIdToken())
                .metadata(metadataFrom(tokenResponse, userInfoResponse, credentials))
                .build();
    }

    private GitLabTokenResponse exchangeCodeForToken(String code, GitLabOAuthClientCredentials credentials) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("client_id", credentials.getClientId());
        body.add("client_secret", credentials.getClientSecret());
        body.add("code", code);
        body.add("redirect_uri", hostResolver.api(REDIRECT_URL));
        body.add("grant_type", "authorization_code");

        try {
            GitLabTokenResponse response = restClient.post()
                    .uri(credentials.getHost() + "/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GitLabTokenResponse.class);

            if (response == null || !StringUtils.hasText(response.getAccessToken())) {
                throw AuthException.of(AuthErrorCode.GITLAB_OAUTH_PROVIDER_ERROR);
            }

            return response;
        } catch (RestClientException exception) {
            throw AuthException.of(AuthErrorCode.GITLAB_OAUTH_PROVIDER_ERROR);
        }
    }

    private GitLabUserInfoResponse fetchUserInfo(String accessToken, GitLabOAuthClientCredentials credentials) {
        Assert.hasText(accessToken, "gitlab access token is required");

        try {
            GitLabUserInfoResponse response = restClient.get()
                    .uri(credentials.getHost() + "/api/v4/user")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(GitLabUserInfoResponse.class);

            if (response == null || response.getId() == null || !StringUtils.hasText(response.getEmail())) {
                throw AuthException.of(AuthErrorCode.GITLAB_OAUTH_PROVIDER_ERROR);
            }

            return response;
        } catch (RestClientException exception) {
            throw AuthException.of(AuthErrorCode.GITLAB_OAUTH_PROVIDER_ERROR);
        }
    }

    private GitLabOAuthClientCredentials requireGitLabClientCredentials() {
        UserAuthRuntimeSecretConfig configurations = configSecretResolver.get();

        Assert.notNull(configurations, "user auth runtime secret config is required");

        String clientId = configurations.getGitlabClientId();
        String clientSecret = configurations.getGitlabClientSecret();
        String host = configurations.getGitlabHost();

        if (!StringUtils.hasText(host)) {
            host = DEFAULT_GITLAB_HOST;
        }

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw AuthException.of(AuthErrorCode.GITLAB_NOT_CONFIGURED);
        }

        return GitLabOAuthClientCredentials.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .host(stripTrailingSlash(host))
                .build();
    }

    private Instant resolveAccessTokenExpiredAt(GitLabTokenResponse response) {
        if (response.getExpiresIn() == null) {
            return null;
        }

        if (response.getCreatedAt() != null) {
            return Instant.ofEpochSecond(response.getCreatedAt()).plusSeconds(response.getExpiresIn());
        }

        return clock.instant().plusSeconds(response.getExpiresIn());
    }

    private String resolveDisplayName(GitLabUserInfoResponse response) {
        if (StringUtils.hasText(response.getName())) {
            return response.getName().trim();
        }

        if (StringUtils.hasText(response.getUsername())) {
            return response.getUsername().trim();
        }

        return response.getEmail();
    }

    private JsonNode metadataFrom(GitLabTokenResponse tokenResponse, GitLabUserInfoResponse userInfoResponse,
            GitLabOAuthClientCredentials credentials) {
        ObjectNode metadata = JsonNodeFactory.instance.objectNode();

        metadata.put("host", credentials.getHost());
        metadata.put("email", userInfoResponse.getEmail());
        metadata.put("username", userInfoResponse.getUsername());
        metadata.put("name", userInfoResponse.getName());
        metadata.put("avatarUrl", userInfoResponse.getAvatarUrl());
        metadata.put("scope", tokenResponse.getScope());
        metadata.put("tokenType", tokenResponse.getTokenType());

        return metadata;
    }

    private static String stripTrailingSlash(String value) {
        String normalized = value.trim();

        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

}
