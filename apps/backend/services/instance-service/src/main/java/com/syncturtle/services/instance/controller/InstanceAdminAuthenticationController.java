package com.syncturtle.services.instance.controller;

import static com.syncturtle.common.core.header.GatewayHeaders.HDR_PREAUTH_TRANSACTION_BINDING;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.cache.response.annotation.InvalidateCache;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.flow.AuthFlow;
import com.syncturtle.common.contracts.auth.session.AdminSessionHandoffResponse;
import com.syncturtle.common.contracts.auth.session.PreAuthTransactionBinding;
import com.syncturtle.common.core.endpoint.EndpointPaths;
import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.instance.controller.validation.AuthFormValidator;
import com.syncturtle.services.instance.dto.request.InstanceAdminSigninForm;
import com.syncturtle.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.services.instance.dto.response.InstanceAdminAuthResponse;
import com.syncturtle.services.instance.service.InstanceAdminAuthenticationService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Profile("!setup")
@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointPaths.API_INSTANCES)
public class InstanceAdminAuthenticationController {

    private final InstanceAdminAuthenticationService service;
    private final ServletAuthCookieWriter cookieWriter;
    private final AuthFormValidator authFormValidator;
    private final PublicUrlResolver hostResolver;

    @InvalidateCache(group = "instance.public-info.v1")
    @InvalidateCache(group = "instance.admins.get")
    @PostMapping(value = EndpointPaths.ADMINS_SIGN__UP, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> instanceAdminSignup(
            @Valid @ModelAttribute InstanceAdminSignupForm form,
            BindingResult bindingResult,
            @RequestHeader(HDR_PREAUTH_TRANSACTION_BINDING) String preAuthBindingHash,
            HttpServletResponse servletResponse) {
        Optional<AuthException> validationFailure = authFormValidator.validate(AuthFlow.INSTANCE_ADMIN_SIGNUP,
                bindingResult, signupPayload(form));

        if (validationFailure.isPresent()) {
            return redirect(hostResolver.adminWithQuery("", validationFailure.get().getErrorMap()));
        }

        PreAuthTransactionBinding preAuthBinding = PreAuthTransactionBinding.fromHash(preAuthBindingHash);
        InstanceAdminAuthResponse response = service.instanceAdminSignup(form, preAuthBinding);
        writeHandoffIfPresent(servletResponse, response.getHandoff());

        return redirect(response.getRedirection());
    }

    @InvalidateCache(group = "instance.public-info.v1")
    @PostMapping(value = EndpointPaths.ADMINS_SIGN__IN, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> instanceAdminSignin(
            @Valid @ModelAttribute InstanceAdminSigninForm form,
            BindingResult bindingResult,
            @RequestHeader(HDR_PREAUTH_TRANSACTION_BINDING) String preAuthBindingHash,
            HttpServletResponse servletResponse) {
        Optional<AuthException> validationFailure = authFormValidator.validate(AuthFlow.INSTANCE_ADMIN_SIGNIN,
                bindingResult, signinPayload(form));

        if (validationFailure.isPresent()) {
            return redirect(hostResolver.adminWithQuery("", validationFailure.get().getErrorMap()));
        }

        PreAuthTransactionBinding preAuthBinding = PreAuthTransactionBinding.fromHash(preAuthBindingHash);
        InstanceAdminAuthResponse response = service.instanceAdminSignin(form, preAuthBinding);
        writeHandoffIfPresent(servletResponse, response.getHandoff());

        return redirect(response.getRedirection());
    }

    private ResponseEntity<Void> redirect(String redirection) {
        Assert.hasText(redirection, "redirection is required");

        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(redirection))
                .build();
    }

    private void writeHandoffIfPresent(HttpServletResponse response, AdminSessionHandoffResponse handoff) {
        if (handoff == null) {
            return;
        }

        Duration maxAge = Duration.between(handoff.getIssuedAt(), handoff.getExpiresAt());
        cookieWriter.setAdminSessionHandoffCookie(response, handoff.getCompletionCode(), maxAge);
    }

    private Map<String, Object> signupPayload(InstanceAdminSignupForm form) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("email", form.getEmail());
        payload.put("firstName", form.getFirstName());
        payload.put("lastName", form.getLastName());
        payload.put("companyName", form.getCompanyName());
        payload.put("telemetryEnabled", form.getTelemetryEnabled());
        payload.put("nextPath", form.getNextPath());

        return payload;
    }

    private Map<String, Object> signinPayload(InstanceAdminSigninForm form) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("email", form.getEmail());

        return payload;
    }

}
