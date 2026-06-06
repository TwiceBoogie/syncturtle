package com.syncturtle.services.user.service.authentication.redirect;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.dto.response.SignOutResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticationRedirector {

    private static final String ADMIN_LOGOUT_CONTEXT = "ADMIN";

    private static final String SIGN_IN_PATH = "/sign-in";
    private static final String SIGN_UP_PATH = "/sign-up";
    private static final String DEFAULT_AUTH_REDIRECT_PATH = "/";

    private final PublicUrlResolver hostResolver;

    public IssueTokenResponse signInFailure(AuthException exception, String nextPath) {
        return redirectTo(SIGN_IN_PATH, exception, nextPath);
    }

    public IssueTokenResponse signUpFailure(AuthException exception, String nextPath) {
        return redirectTo(SIGN_UP_PATH, exception, nextPath);
    }

    public String successLocation(String nextPath) {
        return hostResolver.userApp(safeNextPathOrDefault(nextPath));
    }

    public SignOutResponse signOutRedirect(String logoutContext) {
        boolean adminLogout = ADMIN_LOGOUT_CONTEXT.equalsIgnoreCase(logoutContext);

        String location = adminLogout
                ? hostResolver.admin("")
                : hostResolver.userApp(SIGN_IN_PATH);

        return SignOutResponse.redirect(location);
    }

    private IssueTokenResponse redirectTo(String path, AuthException exception, String nextPath) {
        Assert.hasText(path, "path is required");
        Assert.notNull(exception, "exception is required");

        AuthException enriched = withNextPath(exception, nextPath);

        String location = hostResolver.userAppWithQuery(path, enriched.getErrorMap());

        return IssueTokenResponse.redirect(location);
    }

    private AuthException withNextPath(AuthException exception, String nextPath) {
        Assert.notNull(exception, "exception is required");

        if (StringUtils.hasText(nextPath)) {
            return exception.with("next_path", nextPath);
        }

        return exception;
    }

    private String safeNextPathOrDefault(String nextPath) {
        if (!StringUtils.hasText(nextPath)) {
            return DEFAULT_AUTH_REDIRECT_PATH;
        }

        String normalized = nextPath.trim();

        if (!normalized.startsWith("/")) {
            return DEFAULT_AUTH_REDIRECT_PATH;
        }

        if (normalized.startsWith("//")) {
            return DEFAULT_AUTH_REDIRECT_PATH;
        }

        if (normalized.contains("://")) {
            return DEFAULT_AUTH_REDIRECT_PATH;
        }

        return normalized;
    }
}