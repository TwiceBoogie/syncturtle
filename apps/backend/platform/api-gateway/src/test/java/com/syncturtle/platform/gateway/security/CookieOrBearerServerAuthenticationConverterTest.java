package com.syncturtle.platform.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.platform.gateway.exception.PassportAuthenticationException;

@DisplayName("CookieOrBearerServerAuthenticationConverter")
class CookieOrBearerServerAuthenticationConverterTest {

    private static final String COOKIE_NAME = "access_token";

    private CookieOrBearerServerAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        SecurityCookieFactory cookieFactory = mock(SecurityCookieFactory.class);
        when(cookieFactory.accessCookieName()).thenReturn(COOKIE_NAME);
        converter = new CookieOrBearerServerAuthenticationConverter(cookieFactory);
    }

    @Nested
    @DisplayName("convert(ServerWebExchange)")
    class Convert {

        @Test
        @DisplayName("accepts cookie only credential before isolation")
        void acceptsCookieOnlyCredentialBeforeIsolation() {
            // arrange
            MockServerWebExchange exchange = exchange(null, "cookie-token");
            // conditions
            // act
            Authentication authentication = converter.convert(exchange).block();
            // assert
            assertThat(authentication).isInstanceOf(BearerTokenAuthenticationToken.class);
            assertThat(((BearerTokenAuthenticationToken) authentication).getToken()).isEqualTo("cookie-token");
            // verify
        }

        @Test
        @DisplayName("accepts bearer only credential before isolation")
        void acceptsBearerOnlyCredentialBeforeIsolation() {
            // arrange
            MockServerWebExchange exchange = exchange("header-token", null);
            // conditions
            // act
            Authentication authentication = converter.convert(exchange).block();
            // assert
            assertThat(authentication).isInstanceOf(BearerTokenAuthenticationToken.class);
            assertThat(((BearerTokenAuthenticationToken) authentication).getToken()).isEqualTo("header-token");
            // verify
        }

        @Test
        @DisplayName("accepts the same credential in header and cookie")
        void acceptsTheSameCredentialInHeaderAndCookie() {
            // arrange
            MockServerWebExchange exchange = exchange("same-token", "same-token");
            // conditions
            // act
            Authentication authentication = converter.convert(exchange).block();
            // assert
            assertThat(authentication).isInstanceOf(BearerTokenAuthenticationToken.class);
            assertThat(((BearerTokenAuthenticationToken) authentication).getToken()).isEqualTo("same-token");
            // verify
        }

        @Test
        @DisplayName("rejects different header and cookie credentials")
        void rejectsDifferentHeaderAndCookieCredentials() {
            // arrange
            MockServerWebExchange exchange = exchange("header-token", "cookie-token");
            // conditions
            // act + assert
            assertThatThrownBy(() -> converter.convert(exchange).block())
                    .isInstanceOf(PassportAuthenticationException.class);
            // verify
        }

        @Test
        @DisplayName("rejects duplicate access cookies")
        void rejectsDuplicateAccessCookies() {
            // arrange
            HttpCookie firstCookie = new HttpCookie(COOKIE_NAME, "first");
            HttpCookie secondCookie = new HttpCookie(COOKIE_NAME, "second");
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                    .cookie(firstCookie, secondCookie)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            // conditions
            // act + assert
            assertThatThrownBy(() -> converter.convert(exchange).block())
                    .isInstanceOf(PassportAuthenticationException.class);
            // verify
        }

        @Test
        @DisplayName("rejects duplicate access cookies")
        void rejectsInvalidBearerEvenWhenCookieIsPresent() {
            // arrange
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Basic not-a-bearer")
                    .cookie(new HttpCookie(COOKIE_NAME, "cookie-token"))
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            // conditions
            // act + assert
            assertThatThrownBy(() -> converter.convert(exchange).block()).isInstanceOf(RuntimeException.class);
            // verify
        }

        @Test
        @DisplayName("rejects invalid cookie even when bearer is present")
        void rejectsInvalidCookieEvenWhenBearerIsPresent() {
            // arrange
            MockServerWebExchange exchange = exchange("header-token", "malformed-cookie-token");
            // conditions
            // act + assert
            assertThatThrownBy(() -> converter.convert(exchange).block())
                    .isInstanceOf(PassportAuthenticationException.class);
            // verify
        }
    }

    private MockServerWebExchange exchange(String bearerToken, String cookieToken) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get("/api/users/me");
        if (bearerToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }
        if (cookieToken != null) {
            request.cookie(new HttpCookie(COOKIE_NAME, cookieToken));
        }
        return MockServerWebExchange.from(request.build());
    }
}
