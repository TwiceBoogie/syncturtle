package com.syncturtle.services.user.controllers.client;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.contracts.auth.flow.AuthFlow;
import com.syncturtle.common.core.endpoint.EndpointPaths;
import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.services.user.application.query.AuthenticationQueryHandler;
import com.syncturtle.services.user.dto.request.EmailCheckRequest;
import com.syncturtle.services.user.dto.request.SignInRequest;
import com.syncturtle.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.utils.AuthCookieHelper;
import com.syncturtle.services.user.utils.AuthFormValidator;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointPaths.AUTH)
public class AuthenticationController {

    private final AuthenticationQueryHandler query;
    private final AuthFormValidator validator;
    private final AuthCookieHelper authCookieHelper;

    @PostMapping(EndpointPaths.EMAIL__CHECK)
    public ResponseEntity<EmailCheckResponse> emailCheck(@RequestBody EmailCheckRequest request,
            BindingResult bindingResult) {
        validator.throwIfInvalid(AuthFlow.EMAIL_CHECK, bindingResult, request);

        return ResponseEntity.ok(query.emailCheck(request.getEmail()));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<IssueTokenResponse> signin(@RequestBody SignInRequest request) {
        return null;
    }

    @PostMapping(EndpointPaths.SIGN__OUT)
    public ResponseEntity<Void> signOut(
            @RequestHeader(name = GatewayHeaders.HDR_INTERNAL_LOGOUT_CONTEXT, required = false, defaultValue = "WEB") String logoutContext,
            @RequestHeader(name = "X-Auth-Session-Id", required = false) String sessionId,
            HttpServletResponse servletResponse) {
        authCookieHelper.buildForceLogoutCookieHeaders();

        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(query.signout(logoutContext, sessionId)))
                .build();
    }

}
