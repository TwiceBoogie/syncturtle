package com.syncturtle.services.instance.controllers.client;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.contracts.auth.session.IssueSessionResponse;
import com.syncturtle.common.core.endpoint.EndpointPaths;
import com.syncturtle.common.spring.cache.response.ResponseCache;
import com.syncturtle.common.spring.cache.response.ResponseCacheEvict;
import com.syncturtle.common.spring.web.cookie.ServletAuthCookieWriter;
import com.syncturtle.services.instance.dto.request.InstanceAdminSigninForm;
import com.syncturtle.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.services.instance.dto.response.InstanceAdminAuthResponse;
import com.syncturtle.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.services.instance.services.InstanceAdminService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointPaths.API_INSTANCES)
public class InstanceAdminController {

    private final InstanceAdminService service;
    private final ServletAuthCookieWriter cookieWriter;

    @ResponseCacheEvict(group = "instance.info.get")
    @ResponseCacheEvict(group = "instance.admins.get")
    @PostMapping(value = EndpointPaths.ADMINS_SIGN__UP, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> instanceAdminSignup(
            @Valid @ModelAttribute InstanceAdminSignupForm form,
            HttpServletResponse servletResponse) {
        InstanceAdminAuthResponse response = service.instanceAdminSignup(form);

        setAuthCookies(servletResponse, response.getSession());
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(response.getRedirection()))
                .build();
    }

    @ResponseCacheEvict(group = "instance.info.get")
    @PostMapping(value = EndpointPaths.ADMINS_SIGN__IN, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> instanceAdminSignin(
            @Valid @ModelAttribute InstanceAdminSigninForm form,
            HttpServletResponse servletResponse) {
        InstanceAdminAuthResponse response = service.instanceAdminSignin(form);

        setAuthCookies(servletResponse, response.getSession());
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(response.getRedirection()))
                .build();
    }

    @GetMapping(EndpointPaths.ADMINS)
    @ResponseCache(group = "instance.admins.get", ttlSeconds = 60 * 60 * 2, perUser = false, perWorkspace = false)
    public ResponseEntity<List<InstanceAdminResponse>> getInstanceAdmins() {
        return ResponseEntity.ok(service.getInstanceAdmins());
    }

    private void setAuthCookies(HttpServletResponse servletResponse, IssueSessionResponse session) {
        Duration accessMaxAge = Duration.between(session.getAccessIssuedAt(), session.getAccessExpiresAt());
        Duration refreshMaxAge = Duration.between(session.getRefreshIssuedAt(), session.getRefreshExpiresAt());

        cookieWriter.setAccessTokenCookie(servletResponse, session.getAccessToken(), accessMaxAge);

        cookieWriter.setRefreshTokenCookie(servletResponse, session.getRefreshToken(), refreshMaxAge);
    }

}
