package com.syncturtle.services.user.client.oauth.google;

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
import com.syncturtle.services.user.client.oauth.google.dto.response.GoogleTokenResponse;
import com.syncturtle.services.user.client.oauth.google.dto.response.GoogleUserInfoResponse;
import com.syncturtle.services.user.service.collaborator.authentication.oauth.google.GoogleOAuthClientCredentials;
import com.syncturtle.services.user.service.collaborator.authentication.oauth.google.GoogleOAuthUser;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeSecretConfig;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeSecretResolver;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {

    private static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String SCOPE = "https://www.googleapis.com/auth/userinfo.email "
            + "https://www.googleapis.com/auth/userinfo.profile";
    private static final String REDIRECT_PATH = "/auth/google/callback";

    private final RestClient restClient;
    private final UserAuthRuntimeSecretResolver configSecretResolver;
    private final PublicUrlResolver hostResolver;
    private final Clock clock;

    public String authorizationUrl(String state) {
        Assert.hasText(state, "google oauth state is required");

        GoogleOAuthClientCredentials credentials = requireGoogleClientCredentials();

        return UriComponentsBuilder.fromUriString(AUTHORIZATION_URL)
                .queryParam("client_id", credentials.getClientId())
                .queryParam("scope", SCOPE)
                .queryParam("redirect_uri", hostResolver.api(REDIRECT_PATH))
                .queryParam("response_type", "code")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    public GoogleOAuthUser authenticate(String code) {
        Assert.hasText(code, "google oauth code is required");

        GoogleOAuthClientCredentials credentials = requireGoogleClientCredentials();

        GoogleTokenResponse tokenResponse = exchangeCodeForToken(code, credentials);
        GoogleUserInfoResponse userInfoResponse = fetchUserInfo(tokenResponse.getAccessToken());

        return GoogleOAuthUser.builder()
                .providerAccountId(userInfoResponse.getId())
                .email(userInfoResponse.getEmail())
                .avatarUrl(userInfoResponse.getPicture())
                .firstName(userInfoResponse.getGivenName())
                .lastName(userInfoResponse.getFamilyName())
                .displayName(resolveDisplayName(userInfoResponse))
                .accessToken(tokenResponse.getAccessToken())
                .accessTokenExpiredAt(resolveAccessTokenExpiredAt(tokenResponse))
                .refreshToken(tokenResponse.getRefreshToken())
                .refreshTokenExpiredAt(null)
                .idToken(tokenResponse.getIdToken())
                .metadata(metadataFrom(tokenResponse, userInfoResponse))
                .build();
    }

    private GoogleOAuthClientCredentials requireGoogleClientCredentials() {
        UserAuthRuntimeSecretConfig configurations = configSecretResolver.get();

        Assert.notNull(configurations, "user auth runtime secret config is required");

        String clientId = configurations.getGoogleClientId();
        String clientSecret = configurations.getGoogleClientSecret();

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw AuthException.of(AuthErrorCode.GOOGLE_NOT_CONFIGURED);
        }

        return GoogleOAuthClientCredentials.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }

    private GoogleTokenResponse exchangeCodeForToken(String code, GoogleOAuthClientCredentials credentials) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("code", code);
        body.add("client_id", credentials.getClientId());
        body.add("client_secret", credentials.getClientSecret());
        body.add("redirect_uri", hostResolver.api(REDIRECT_PATH));
        body.add("grant_type", "authorization_code");

        try {
            GoogleTokenResponse response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(GoogleTokenResponse.class);

            if (response == null || !StringUtils.hasText(response.getAccessToken())) {
                throw AuthException.of(AuthErrorCode.GOOGLE_OAUTH_PROVIDER_ERROR);
            }

            return response;
        } catch (RestClientException exception) {
            throw AuthException.of(AuthErrorCode.GOOGLE_OAUTH_PROVIDER_ERROR);
        }
    }

    private GoogleUserInfoResponse fetchUserInfo(String accessToken) {
        Assert.hasText(accessToken, "google accessToken is required");

        try {
            GoogleUserInfoResponse response = restClient.get()
                    .uri(USERINFO_URL)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(GoogleUserInfoResponse.class);

            if (response == null || !StringUtils.hasText(response.getId())
                    || !StringUtils.hasText(response.getEmail())) {
                throw AuthException.of(AuthErrorCode.GOOGLE_OAUTH_PROVIDER_ERROR);
            }

            return response;
        } catch (RestClientException exception) {
            throw AuthException.of(AuthErrorCode.GOOGLE_OAUTH_PROVIDER_ERROR);
        }
    }

    private Instant resolveAccessTokenExpiredAt(GoogleTokenResponse response) {
        if (response.getExpiresIn() == null) {
            return null;
        }

        return clock.instant().plusSeconds(response.getExpiresIn());
    }

    private String resolveDisplayName(GoogleUserInfoResponse response) {
        String firstName = response.getGivenName() == null ? "" : response.getGivenName().trim();
        String lastName = response.getFamilyName() == null ? "" : response.getFamilyName().trim();

        String fullName = (firstName + " " + lastName).trim();

        if (StringUtils.hasText(fullName)) {
            return fullName;
        }

        return response.getEmail();
    }

    private JsonNode metadataFrom(GoogleTokenResponse tokenResponse, GoogleUserInfoResponse userInfoResponse) {
        ObjectNode metadata = JsonNodeFactory.instance.objectNode();

        metadata.put("email", userInfoResponse.getEmail());
        metadata.put("picture", userInfoResponse.getPicture());
        metadata.put("givenName", userInfoResponse.getGivenName());
        metadata.put("familyName", userInfoResponse.getFamilyName());
        metadata.put("scope", tokenResponse.getScope());
        metadata.put("tokenType", tokenResponse.getTokenType());

        return metadata;
    }

}
