package com.syncturtle.platform.services.instance.controllers.client;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.core.constants.GatewayHeaderNames;
import com.syncturtle.common.core.enums.AuthFlow;
import com.syncturtle.common.spring.cache.response.ResponseCache;
import com.syncturtle.common.spring.cache.response.ResponseCacheEvict;
import com.syncturtle.common.spring.security.authz.AllowAnonymous;
import com.syncturtle.common.spring.security.authz.RequireInstanceAdmin;
import com.syncturtle.platform.services.instance.application.query.InstanceInfoQueryHandler;
import com.syncturtle.platform.services.instance.dto.internal.InstanceAdminSignupResult;
import com.syncturtle.platform.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.platform.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.platform.services.instance.dto.response.InstanceInfo;
import com.syncturtle.platform.services.instance.dto.response.UserMeResponse;
import com.syncturtle.platform.services.instance.utils.validation.AuthFormValidator;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/instances")
@RequireInstanceAdmin(minRole = 15)
public class InstanceController {

    private final InstanceInfoQueryHandler query;
    private final AuthFormValidator authValidator;

    @GetMapping
    @AllowAnonymous
    @ResponseCache(group = "instance.info.get", ttlSeconds = 60 * 60 * 2, perUser = false, perWorkspace = false)
    public ResponseEntity<InstanceInfo> getInstanceInfo() {
        return ResponseEntity.ok(query.getInstanceInfo());
    }

    @PatchMapping
    @ResponseCacheEvict(group = "instance.info.get", beforeInvocation = true)
    public ResponseEntity<?> updateInstanceInfo() {
        return null;
    }

    @GetMapping("/admins")
    @ResponseCache(group = "instance.admins.get", ttlSeconds = 60 * 60 * 2, perUser = false, perWorkspace = false)
    public ResponseEntity<List<InstanceAdminResponse>> getInstanceAdmins() {
        return ResponseEntity.ok(query.getInstanceAdmins());
    }

    @GetMapping("/admins/me")
    public ResponseEntity<UserMeResponse> getInstanceAdminUserMe() {
        return ResponseEntity.ok(query.getInstanceAdminUserMe());
    }

    @AllowAnonymous
    @ResponseCacheEvict(group = "instance.info.get")
    @PostMapping(value = "/admins/sign-up", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> instanceAdminSignup(@Valid @ModelAttribute InstanceAdminSignupForm form,
            BindingResult bindingResult) {
        authValidator.throwIfInvalid(AuthFlow.INSTANCE_ADMIN_SIGNUP, bindingResult, form);

        InstanceAdminSignupResult result = query.instanceAdminSignup(form);
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .header(GatewayHeaderNames.HDR_INTERNAL_USER_ID, result.getUserId().toString())
                .header(GatewayHeaderNames.HDR_AUTH_SESSION_TYPE, "ADMIN")
                .location(URI.create(result.getRedirectLocation()))
                .build();
    }

    @AllowAnonymous
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("hello");
    }
}
