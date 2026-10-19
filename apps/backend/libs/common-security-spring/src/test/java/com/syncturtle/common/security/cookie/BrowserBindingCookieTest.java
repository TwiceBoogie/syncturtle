package com.syncturtle.common.security.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import com.syncturtle.common.security.property.SecurityCookieProperties;

class BrowserBindingCookieTest {

    @Test
    void managedBrowserBindingsAreHostOnlyHttpOnlyAndHaveCallbackAppropriateSameSite() {
        SecurityCookieFactory factory = new SecurityCookieFactory(
                new SecurityCookieProperties(true, true, "Lax", "Lax", "Lax"));
        ResponseCookie admin = factory.adminHandoffBindingCookie("admin-secret", Duration.ofSeconds(30));
        ResponseCookie oauth = factory.oauthBindingCookie("oauth-secret", Duration.ofMinutes(10));

        assertThat(admin.getName()).isEqualTo("__Host-st_admin_handoff_binding");
        assertThat(admin.getPath()).isEqualTo("/");
        assertThat(admin.getDomain()).isNull();
        assertThat(admin.isSecure()).isTrue();
        assertThat(admin.isHttpOnly()).isTrue();
        assertThat(admin.getSameSite()).isEqualTo("Strict");
        assertThat(oauth.getName()).isEqualTo("__Host-st_oauth_browser_binding");
        assertThat(oauth.isHttpOnly()).isTrue();
        assertThat(oauth.getSameSite()).isEqualTo("Lax");
        assertThat(factory.clearAdminHandoffBindingCookie().getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(factory.clearOauthBindingCookie().getMaxAge()).isEqualTo(Duration.ZERO);
    }

}
