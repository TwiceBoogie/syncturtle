package com.syncturtle.services.user.controllers.client;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.contracts.auth.flow.AuthFlow;
import com.syncturtle.common.core.endpoint.EndpointPaths;
import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.spring.web.cookie.ServletAuthCookieWriter;
import com.syncturtle.services.user.dto.internal.RefreshExchangeResult;
import com.syncturtle.services.user.dto.request.EmailCheckRequest;
import com.syncturtle.services.user.dto.request.SignInRequest;
import com.syncturtle.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.services.AuthenticationService;
import com.syncturtle.services.user.utils.AuthFormValidator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.syncturtle.common.core.cookie.CookieNames.COOKIE_NAME_REFRESH_TOKEN;

import java.net.URI;
import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointPaths.AUTH)
public class AuthenticationController {

    private static final String HOST_PREFIX = "__Host-";

    private final AuthenticationService service;
    private final AuthFormValidator validator;
    private final ServletAuthCookieWriter cookieWriter;

    @PostMapping(EndpointPaths.EMAIL__CHECK)
    public ResponseEntity<EmailCheckResponse> emailCheck(@RequestBody EmailCheckRequest request,
            BindingResult bindingResult) {
        validator.throwIfInvalid(AuthFlow.EMAIL_CHECK, bindingResult, request);

        return ResponseEntity.ok(service.emailCheck(request.getEmail()));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<IssueTokenResponse> signin(@RequestBody SignInRequest request) {
        return null;
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            HttpServletRequest request,
            HttpServletResponse response,
            @CookieValue(name = COOKIE_NAME_REFRESH_TOKEN, required = false) String refreshToken,
            @CookieValue(name = HOST_PREFIX + COOKIE_NAME_REFRESH_TOKEN, required = false) String hostRefreshToken) {
        String presentedRefreshToken = firstText(refreshToken, hostRefreshToken);

        if (!StringUtils.hasText(presentedRefreshToken)) {
            cookieWriter.clearAuthCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RefreshExchangeResult result = service.refreshSession(presentedRefreshToken);

        Duration accessMaxAge = Duration.between(
                result.getAccess().getIssuedAt(),
                result.getAccess().getExpiresAt());
        Duration refreshMaxAge = Duration.between(
                result.getRefresh().getIssuedAt(),
                result.getRefresh().getExpiresAt());
        cookieWriter.setAuthCookies(
                response,
                result.getAccess().getToken(),
                accessMaxAge,
                result.getRefresh().getToken(), refreshMaxAge);

        return ResponseEntity.noContent().build();
    }

    @PostMapping(EndpointPaths.SIGN__OUT)
    public ResponseEntity<Void> signOut(
            @RequestHeader(name = GatewayHeaders.HDR_INTERNAL_LOGOUT_CONTEXT, required = false, defaultValue = "WEB") String logoutContext,
            @RequestHeader(name = "X-Auth-Session-Id", required = false) String sessionId,
            HttpServletResponse servletResponse) {
        cookieWriter.clearAllSecurityCookies(servletResponse);
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(service.signOut(logoutContext, sessionId)))
                .build();
    }

    private String firstText(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }

        return second;
    }

}
