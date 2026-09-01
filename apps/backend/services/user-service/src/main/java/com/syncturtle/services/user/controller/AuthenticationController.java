package com.syncturtle.services.user.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.cache.response.annotation.InvalidateCache;
import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.flow.AuthFlow;
import com.syncturtle.common.core.endpoint.EndpointPaths;
import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.web.annotation.CurrentUser;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.user.controller.validation.AuthFormValidator;
import com.syncturtle.services.user.dto.request.EmailCheckRequest;
import com.syncturtle.services.user.dto.request.MagicSignInRequest;
import com.syncturtle.services.user.dto.request.MagicSignUpRequest;
import com.syncturtle.services.user.dto.request.SetPasswordRequest;
import com.syncturtle.services.user.dto.request.SignInRequest;
import com.syncturtle.services.user.dto.request.SignUpRequest;
import com.syncturtle.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.dto.response.IssueTokenWithUserResponse;
import com.syncturtle.services.user.dto.response.MagicCodeResponse;
import com.syncturtle.services.user.dto.response.SignOutResponse;
import com.syncturtle.services.user.dto.response.UserMeResponse;
import com.syncturtle.services.user.service.AuthenticationService;
import com.syncturtle.services.user.service.RefreshSessionService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointPaths.AUTH)
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final RefreshSessionService refreshSessionService;
    private final ServletAuthCookieWriter cookieWriter;
    private final AuthFormValidator authFormValidator;
    private final PublicUrlResolver hostResolver;

    @PostMapping(EndpointPaths.EMAIL__CHECK)
    public ResponseEntity<EmailCheckResponse> emailCheck(@RequestBody EmailCheckRequest request) {
        return ResponseEntity.ok(authenticationService.emailCheck(request.getEmail()));
    }

    @PostMapping("/magic-generate")
    public ResponseEntity<MagicCodeResponse> generateMagicCode(@Valid @RequestBody EmailCheckRequest request) {
        return ResponseEntity.ok(authenticationService.generateMagicCode(request.getEmail()));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<Void> signin(
            @Valid @ModelAttribute SignInRequest request,
            BindingResult bindingResult,
            HttpServletResponse servletResponse) {
        Optional<AuthException> validationFailure = authFormValidator.validate(
                AuthFlow.REGULAR_SIGN_IN,
                bindingResult,
                signinPayload(request));

        if (validationFailure.isPresent()) {
            return redirect(hostResolver.userAppWithQuery("/sign-in", validationFailure.get().getErrorMap()));
        }

        IssueTokenResponse response = authenticationService.emailPasswordSignIn(
                request.getEmail(),
                request.getPassword(),
                request.getNextPath());
        writeSessionCookiesIfPresent(servletResponse, response, true);
        return redirect(response.getLocation());
    }

    @PostMapping("/sign-up")
    public ResponseEntity<Void> signup(
            @Valid @ModelAttribute SignUpRequest request,
            BindingResult bindingResult,
            HttpServletResponse servletResponse) {
        Optional<AuthException> validationFailure = authFormValidator.validate(
                AuthFlow.REGULAR_SIGN_UP,
                bindingResult,
                signupPayload(request));

        if (validationFailure.isPresent()) {
            return redirect(hostResolver.userAppWithQuery("", validationFailure.get().getErrorMap()));
        }

        IssueTokenResponse response = authenticationService.emailPasswordSignUp(
                request.getEmail(),
                request.getPassword(),
                request.getNextPath());
        writeSessionCookiesIfPresent(servletResponse, response, true);
        return redirect(response.getLocation());
    }

    @PostMapping("/magic-sign-in")
    public ResponseEntity<Void> magicSignin(@ModelAttribute MagicSignInRequest request,
            HttpServletResponse servletResponse) {
        IssueTokenResponse response = authenticationService.magicCodeSignIn(
                request.getEmail(),
                request.getCode(),
                request.getNextPath());
        writeSessionCookiesIfPresent(servletResponse, response, true);
        return redirect(response.getLocation());
    }

    @PostMapping("/magic-sign-up")
    public ResponseEntity<Void> magicSignup(@ModelAttribute MagicSignUpRequest request,
            HttpServletResponse servletResponse) {
        IssueTokenResponse response = authenticationService.magicCodeSignUp(
                request.getEmail(),
                request.getCode(),
                request.getNextPath());
        writeSessionCookiesIfPresent(servletResponse, response, true);
        return redirect(response.getLocation());
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            HttpServletResponse servletResponse,
            @RequestHeader(name = GatewayHeaders.HDR_INTERNAL_CSRF_SESSION_ID) String trustedCsrfSessionId,
            @CookieValue(name = "#{@securityCookieFactory.refreshCookieName()}", required = false) String presentedRefreshToken) {
        if (!StringUtils.hasText(presentedRefreshToken)) {
            cookieWriter.clearAuthCookies(servletResponse);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            IssueTokenResponse response = refreshSessionService.refreshSession(presentedRefreshToken,
                    trustedCsrfSessionId);
            writeSessionCookiesIfPresent(servletResponse, response, false);

            return ResponseEntity.noContent().build();
        } catch (AuthException exception) {
            if (exception.getAuthErrorCode() == AuthErrorCode.AUTHENTICATION_FAILED) {
                cookieWriter.clearAuthCookies(servletResponse);
            }

            if (exception.getAuthErrorCode() == AuthErrorCode.INVALID_CSRF_TOKEN) {
                cookieWriter.clearAllSecurityCookies(servletResponse);
            }

            throw exception;
        }
    }

    @PostMapping(EndpointPaths.SIGN__OUT)
    public ResponseEntity<Void> signOut(
            @RequestHeader(name = GatewayHeaders.HDR_INTERNAL_LOGOUT_CONTEXT, required = false, defaultValue = "WEB") String logoutContext,
            @RequestHeader(name = GatewayHeaders.HDR_INTERNAL_CSRF_SESSION_ID) String trustedCsrfSessionId,
            @CookieValue(name = "#{@securityCookieFactory.refreshCookieName()}", required = false) String presentedRefreshToken,
            HttpServletResponse servletResponse) {
        try {
            SignOutResponse response = authenticationService.signOut(logoutContext, presentedRefreshToken,
                    trustedCsrfSessionId);
            cookieWriter.clearAllSecurityCookies(servletResponse);
            cookieWriter.clearAdminSessionHandoffCookie(servletResponse);

            return ResponseEntity.status(HttpStatus.SEE_OTHER)
                    .location(URI.create(response.getRedirection()))
                    .build();
        } catch (AuthException exception) {
            if (exception.getAuthErrorCode() == AuthErrorCode.INVALID_CSRF_TOKEN) {
                cookieWriter.clearAllSecurityCookies(servletResponse);
                cookieWriter.clearAdminSessionHandoffCookie(servletResponse);
            }

            throw exception;
        }
    }

    @PostMapping("/set-password")
    @InvalidateCache(group = "user-me.v1")
    public ResponseEntity<UserMeResponse> setPassword(@CurrentUser UUID currentUserId,
            @RequestHeader(name = GatewayHeaders.HDR_AUTH_SESSION_ID, required = false) String sessionId,
            @Valid @RequestBody SetPasswordRequest request,
            HttpServletResponse servletResponse) {
        IssueTokenWithUserResponse response = authenticationService.setPassword(
                currentUserId,
                sessionId,
                request.getPassword());
        cookieWriter.clearAuthCookies(servletResponse);
        writeSessionCookiesIfPresent(servletResponse, response.getTokens(), false);
        return ResponseEntity.ok(response.getUser());
    }

    private void writeSessionCookiesIfPresent(HttpServletResponse response, IssueTokenResponse session,
            boolean clearCsrf) {
        if (!hasIssuedSession(session)) {
            return;
        }

        if (clearCsrf) {
            cookieWriter.clearCsrfCookie(response);
        }

        Duration accessMaxAge = Duration.between(session.getAccessIssuedAt(), session.getAccessExpiresAt());
        Duration refreshMaxAge = Duration.between(session.getRefreshIssuedAt(), session.getRefreshExpiresAt());

        cookieWriter.setAuthCookies(
                response,
                session.getAccessToken(),
                accessMaxAge,
                session.getRefreshToken(),
                refreshMaxAge);
    }

    private static boolean hasIssuedSession(IssueTokenResponse session) {
        return session != null
                && StringUtils.hasText(session.getAccessToken())
                && session.getAccessIssuedAt() != null
                && session.getAccessExpiresAt() != null
                && StringUtils.hasText(session.getRefreshToken())
                && session.getRefreshIssuedAt() != null
                && session.getRefreshExpiresAt() != null;
    }

    private ResponseEntity<Void> redirect(String redirection) {
        Assert.hasText(redirection, "redirection is required");

        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(redirection))
                .build();
    }

    private Map<String, Object> signinPayload(SignInRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("email", request.getEmail());
        payload.put("nextPath", request.getNextPath());

        return payload;
    }

    private Map<String, Object> signupPayload(SignUpRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("email", request.getEmail());
        payload.put("nextPath", request.getNextPath());

        return payload;
    }

}
