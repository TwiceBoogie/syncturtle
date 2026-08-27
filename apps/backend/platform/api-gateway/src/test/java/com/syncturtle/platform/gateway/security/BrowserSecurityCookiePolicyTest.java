package com.syncturtle.platform.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import com.syncturtle.common.security.cookie.SecurityCookieFactory;
import com.syncturtle.common.security.property.SecurityCookieProperties;
import com.syncturtle.platform.gateway.security.BrowserSecurityCookiePolicy.Decision;

@DisplayName("BrowserSecurityCookiePolicy")
class BrowserSecurityCookiePolicyTest {

    private BrowserSecurityCookiePolicy policy;

    @BeforeEach
    void setup() {
        SecurityCookieProperties properties = new SecurityCookieProperties(
                false,
                false,
                "Lax",
                "Lax",
                "Lax",
                Duration.ofMinutes(30));
        policy = new BrowserSecurityCookiePolicy(new SecurityCookieFactory(properties));
    }

    @Nested
    @DisplayName("refresh")
    class RefreshTests {

        @Test
        @DisplayName("canonicalizes one current or legacy candidate")
        void canonicalizesOneCurrentOrLegacyCandidate() {
            // arrange
            MockServerHttpRequest current = post("/auth/refresh")
                    .cookie(new HttpCookie("refresh_token", "current"))
                    .build();
            MockServerHttpRequest legacy = post("/auth/refresh/")
                    .cookie(new HttpCookie("__Host-refresh_token", "legacy"))
                    .build();
            // conditions
            // act
            Decision currentDecision = policy.decide(current);
            Decision legacyDecision = policy.decide(legacy);
            // assert
            assertThat(currentDecision.getDownstreamCookies())
                    .extracting(HttpCookie::toString)
                    .containsExactly("refresh_token=current");
            assertThat(legacyDecision.getDownstreamCookies())
                    .extracting(HttpCookie::toString)
                    .containsExactly("refresh_token=legacy");
            // verify
        }

        @Test
        @DisplayName("treats one blank candidate as missing")
        void treatsOneBlankCandidateAsMissing() {
            // arrange
            MockServerHttpRequest request = post("/auth/refresh")
                    .cookie(new HttpCookie("refresh_token", ""))
                    .build();
            // conditions
            // act
            Decision decision = policy.decide(request);
            // assert
            assertThat(decision.isRefreshConflict()).isFalse();
            assertThat(decision.getDownstreamCookies()).isEmpty();
            // verify
        }

        @Test
        @DisplayName("rejects repeated same name without depending on order")
        void rejectsRepeatedSameNameWithoutDependingOnOrder() {
            // arrange
            HttpCookie firstCookie = new HttpCookie("refresh_token", "first");
            HttpCookie secondCookie = new HttpCookie("refresh_token", "second");
            MockServerHttpRequest firstOrder = post("/auth/refresh")
                    .cookie(firstCookie, secondCookie)
                    .build();
            MockServerHttpRequest reverseOrder = post("/auth/refresh")
                    .cookie(secondCookie, firstCookie)
                    .build();
            // conditions
            // act
            Decision firstDecision = policy.decide(firstOrder);
            Decision reverseDecision = policy.decide(reverseOrder);
            // assert
            assertThat(firstDecision.isRefreshConflict()).isTrue();
            assertThat(reverseDecision.isRefreshConflict()).isTrue();
            // verify
        }

        @Test
        @DisplayName("rejects cross prefix and blank value combinations")
        void rejectsCrossPrefixAndBlankValueCombinations() {
            HttpCookie token1 = new HttpCookie("refresh_token", "current");
            HttpCookie token2 = new HttpCookie("__Secure-refresh_token", "managed");
            HttpCookie token3 = new HttpCookie("refresh_token", "");
            HttpCookie token4 = new HttpCookie("__Host-refresh_token", "legacy");
            MockServerHttpRequest crossPrefix = post("/auth/refresh")
                    .cookie(token1, token2)
                    .build();
            MockServerHttpRequest blankAndValue = post("/auth/refresh")
                    .cookie(token3, token4)
                    .build();
            // act
            Decision crossPrefixDecision = policy.decide(crossPrefix);
            Decision blankAndValueDecision = policy.decide(blankAndValue);
            // assert
            assertThat(crossPrefixDecision.isRefreshConflict()).isTrue();
            assertThat(blankAndValueDecision.isRefreshConflict()).isTrue();
            // verify
        }

        @Test
        @DisplayName("does not forward csrf or unknown cookies on json refresh")
        void doesNotForwardCsrfOrUnknownCookiesOnJsonRefresh() {
            // arrange
            HttpCookie refreshToken = new HttpCookie("refresh_token", "refresh");
            HttpCookie csrfToken = new HttpCookie("csrf_token", "csrf");
            HttpCookie themeToken = new HttpCookie("theme", "dark");
            MockServerHttpRequest request = post("/auth/refresh")
                    .cookie(refreshToken, csrfToken, themeToken)
                    .build();
            // conditions
            // act
            Decision decision = policy.decide(request);
            // assert
            assertThat(decision.getDownstreamCookies())
                    .extracting(HttpCookie::toString)
                    .containsExactly("refresh_token=refresh");
            // verify
        }

    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("forwards refresh and forms csrf on both final logout routes")
        void forwardsRefreshAndFormCsrfOnBothFinalLogoutRoutes() {
            // arrange
            HttpCookie secureRefreshToken = new HttpCookie("__Secure-refresh_token", "refresh");
            HttpCookie refreshToken = new HttpCookie("refresh_token", "refresh");
            HttpCookie csrfToken = new HttpCookie("csrf_token", "csrf");
            MockServerHttpRequest normal = formPost("/auth/sign-out")
                    .cookie(secureRefreshToken, csrfToken)
                    .build();
            MockServerHttpRequest admin = formPost("/auth/admin/sign-out/")
                    .cookie(refreshToken, csrfToken)
                    .build();
            // conditions
            // act
            Decision normalDecision = policy.decide(normal);
            Decision adminDecision = policy.decide(admin);
            // assert
            assertThat(normalDecision.getDownstreamCookies())
                    .extracting(HttpCookie::toString)
                    .containsExactly("refresh_token=refresh", "csrf_token=csrf");
            assertThat(adminDecision.getDownstreamCookies())
                    .extracting(HttpCookie::toString)
                    .containsExactly("refresh_token=refresh", "csrf_token=csrf");
            // verify
        }

        @Test
        @DisplayName("conflict forwards no refresh but still allows browser logout")
        void conflictForwardsNoRefreshButStillAllowsBrowserLogout() {
            // arrange
            HttpCookie refreshToken = new HttpCookie("refresh_token", "one");
            HttpCookie hostRefreshToken = new HttpCookie("__Host-refresh_token", "two");
            HttpCookie csrfToken = new HttpCookie("csrf_token", "csrf");
            MockServerHttpRequest request = formPost("/auth/sign-out")
                    .cookie(refreshToken, hostRefreshToken, csrfToken)
                    .build();
            // conditions
            // act
            Decision decision = policy.decide(request);
            // assert
            assertThat(decision.isRefreshConflict()).isFalse();
            assertThat(decision.getDownstreamCookies())
                    .extracting(HttpCookie::toString)
                    .containsExactly("csrf_token=csrf");
            // verify
        }

    }

    @Nested
    @DisplayName("transitional csrf and handoff")
    class TransitionalCsrfAndHandoffTests {

        @Test
        @DisplayName("completion gets exactly one handoff and one csrf cookie")
        void completionGetsExactlyOneHandoffAndOneCsrfCookie() {
            // arrange
            HttpCookie accessToken = new HttpCookie("access_token", "access");
            HttpCookie refreshToken = new HttpCookie("refresh_token", "refresh");
            HttpCookie handoffToken = new HttpCookie("admin_session_handoff", "handoff");
            HttpCookie csrfToken = new HttpCookie("csrf_token", "csrf");
            MockServerHttpRequest request = MockServerHttpRequest.get("/auth/admin/session")
                    .cookie(accessToken, refreshToken, handoffToken, csrfToken)
                    .build();
            // conditions
            // act
            Decision decision = policy.decide(request);
            // assert
            assertThat(decision.getDownstreamCookies())
                    .extracting(HttpCookie::toString)
                    .containsExactly("admin_session_handoff=handoff", "csrf_token=csrf");
            // verify
        }

        @Test
        @DisplayName("forwards csrf only for exact current form routes")
        void forwardsCsrfOnlyForExactCurrentFormRoutes() {
            // arrange
            HttpCookie csrfToken = new HttpCookie("csrf_token", "csrf");
            MockServerHttpRequest exact = formPost("/auth/forgot-password/")
                    .cookie(csrfToken)
                    .build();
            MockServerHttpRequest dynamicReset = formPost("/auth/reset-password/user/token")
                    .cookie(csrfToken)
                    .build();
            MockServerHttpRequest nearMatch = formPost("/auth/sign-in/extra")
                    .cookie(csrfToken)
                    .build();
            // conditions
            // act
            Decision exactDecision = policy.decide(exact);
            Decision dynamicDecision = policy.decide(dynamicReset);
            Decision nearMatchDecision = policy.decide(nearMatch);
            // assert
            assertThat(exactDecision.getDownstreamCookies())
                    .extracting(HttpCookie::toString)
                    .containsExactly("csrf_token=csrf");
            assertThat(dynamicDecision.getDownstreamCookies()).isEmpty();
            assertThat(nearMatchDecision.getDownstreamCookies()).isEmpty();
            // verify
        }

        @Test
        @DisplayName("defaults unknown json options and ordinary routes to no cookies")
        void defaultsUnknownJsonOptionsAndOrdinaryRoutesToNoCookies() {
            // arrange
            HttpCookie accessToken = new HttpCookie("access_token", "access");
            HttpCookie refreshToken = new HttpCookie("refresh_token", "refresh");
            HttpCookie themeToken = new HttpCookie("theme", "dark");
            MockServerHttpRequest ordinary = MockServerHttpRequest.get("/api/users/me")
                    .cookie(accessToken, themeToken)
                    .build();
            MockServerHttpRequest options = MockServerHttpRequest.options("/auth/refresh")
                    .cookie(refreshToken)
                    .build();
            // conditions
            // act
            Decision ordinaryDecision = policy.decide(ordinary);
            Decision optionsDecision = policy.decide(options);
            // assert
            assertThat(ordinaryDecision.getDownstreamCookies()).isEmpty();
            assertThat(optionsDecision.getDownstreamCookies()).isEmpty();
            // verify
        }

        @Test
        @DisplayName("forwards no cookies to session inventory or revocation routes")
        void forwardsNoCookiesToSessionInventoryOrRevocationRoutes() {
            // arrange
            HttpCookie accessToken = new HttpCookie("access_token", "access");
            HttpCookie refreshToken = new HttpCookie("refresh_token", "refresh");
            HttpCookie csrfToken = new HttpCookie("csrf_token", "csrf");
            MockServerHttpRequest inventory = MockServerHttpRequest.get("/api/users/me/sessions")
                    .cookie(accessToken, refreshToken, csrfToken)
                    .build();
            MockServerHttpRequest revocation = MockServerHttpRequest
                    .delete("/api/users/me/sessions/others")
                    .cookie(accessToken, refreshToken, csrfToken)
                    .build();
            // conditions
            // act
            Decision inventoryDecision = policy.decide(inventory);
            Decision revocationDecision = policy.decide(revocation);
            // assert
            assertThat(inventoryDecision.getDownstreamCookies()).isEmpty();
            assertThat(revocationDecision.getDownstreamCookies()).isEmpty();
            // verify
        }

    }

    private static MockServerHttpRequest.BodyBuilder post(String path) {
        return MockServerHttpRequest.post(path);
    }

    private static MockServerHttpRequest.BodyBuilder formPost(String path) {
        return post(path).contentType(MediaType.APPLICATION_FORM_URLENCODED);
    }

}
