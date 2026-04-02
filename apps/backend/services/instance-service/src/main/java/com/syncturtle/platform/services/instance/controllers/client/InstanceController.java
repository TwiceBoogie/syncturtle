package com.syncturtle.platform.services.instance.controllers.client;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.core.constants.EndpointConstants;
import com.syncturtle.common.core.constants.GatewayHeaderNames;
import com.syncturtle.common.core.enums.AuthFlow;
import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.common.spring.cache.response.ResponseCache;
import com.syncturtle.common.spring.cache.response.ResponseCacheEvict;
import com.syncturtle.common.spring.security.authz.AllowAnonymous;
import com.syncturtle.common.spring.security.authz.RequireInstanceAdmin;
import com.syncturtle.platform.services.instance.application.query.InstanceQueryHandler;
import com.syncturtle.platform.services.instance.dto.internal.InstanceAdminSigninResult;
import com.syncturtle.platform.services.instance.dto.internal.InstanceAdminSignupResult;
import com.syncturtle.platform.services.instance.dto.request.InstanceAdminSigninForm;
import com.syncturtle.platform.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.platform.services.instance.dto.request.InstanceRequest;
import com.syncturtle.platform.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.platform.services.instance.dto.response.InstanceConfigurationResponse;
import com.syncturtle.platform.services.instance.dto.response.InstanceInfo;
import com.syncturtle.platform.services.instance.dto.response.InstanceResponse;
import com.syncturtle.platform.services.instance.dto.response.UserMeResponse;
import com.syncturtle.platform.services.instance.utils.validation.AuthFormValidator;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointConstants.API_INSTANCES)
@RequireInstanceAdmin(minRole = 15)
public class InstanceController {

    private final InstanceQueryHandler query;
    private final AuthFormValidator authValidator;

    @GetMapping
    @AllowAnonymous
    @ResponseCache(group = "instance.info.get", ttlSeconds = 60 * 60 * 2, perUser = false, perWorkspace = false)
    public ResponseEntity<InstanceInfo> getInstanceInfo() {
        return ResponseEntity.ok(query.getInstanceInfo());
    }

    @PatchMapping
    @ResponseCacheEvict(group = "instance.info.get", beforeInvocation = true)
    public ResponseEntity<InstanceResponse> updateInstanceInfo(@Valid @RequestBody InstanceRequest request) {
        return ResponseEntity.ok(query.instanceUpdate(request));
    }

    @GetMapping(EndpointConstants.CONFIGURATIONS)
    @ResponseCache(group = "instance.config.get", ttlSeconds = 60 * 60 * 2, perUser = false, perWorkspace = false)
    public ResponseEntity<List<InstanceConfigurationResponse>> configurationsAll() {
        return ResponseEntity.ok(query.configurationsAll());
    }

    @PatchMapping(EndpointConstants.CONFIGURATIONS)
    @ResponseCacheEvict(group = "instance.config.get")
    @ResponseCacheEvict(group = "instance.info.get")
    public ResponseEntity<List<InstanceConfigurationResponse>> configurationsUpdate(
            @RequestBody Map<InstanceConfigurationKey, String> request) {
        return ResponseEntity.ok(query.configurationUpdate(request));
    }

    @GetMapping(EndpointConstants.ADMINS)
    @ResponseCache(group = "instance.admins.get", ttlSeconds = 60 * 60 * 2, perUser = false, perWorkspace = false)
    public ResponseEntity<List<InstanceAdminResponse>> getInstanceAdmins() {
        return ResponseEntity.ok(query.getInstanceAdmins());
    }

    @GetMapping(EndpointConstants.ADMINS_ME)
    public ResponseEntity<UserMeResponse> getInstanceAdminUserMe() {
        return ResponseEntity.ok(query.getInstanceAdminUserMe());
    }

    @AllowAnonymous
    @ResponseCacheEvict(group = "instance.info.get")
    @PostMapping(value = EndpointConstants.ADMINS_SIGN__UP, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> instanceAdminSignup(@Valid @ModelAttribute InstanceAdminSignupForm form,
            BindingResult bindingResult) {
        authValidator.throwIfInvalid(AuthFlow.INSTANCE_ADMIN_SIGNUP, bindingResult,
                form);

        InstanceAdminSignupResult result = query.instanceAdminSignup(form);
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .header(GatewayHeaderNames.HDR_INTERNAL_USER_ID,
                        result.getUserId().toString())
                .header(GatewayHeaderNames.HDR_AUTH_SESSION_TYPE, "ADMIN")
                .location(URI.create(result.getRedirectLocation()))
                .build();
    }

    @AllowAnonymous
    @ResponseCacheEvict(group = "instance.info.get")
    @PostMapping(value = EndpointConstants.ADMINS_SIGN__IN, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> instanceAdminsSignin(@Valid @ModelAttribute InstanceAdminSigninForm form,
            BindingResult bindingResult) {
        authValidator.throwIfInvalid(AuthFlow.INSTANCE_ADMIN_SIGNIN, bindingResult, form);

        InstanceAdminSigninResult result = query.instanceAdminSignin(form);
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .header(GatewayHeaderNames.HDR_INTERNAL_USER_ID,
                        result.getUserId() == null ? "" : result.getUserId().toString())
                .header(GatewayHeaderNames.HDR_AUTH_SESSION_TYPE, "ADMIN")
                .location(URI.create(result.getRedirectionLocation()))
                .build();
    }

    @DeleteMapping(EndpointConstants.CONFIGURATIONS_DISABLE_EMAIL_FEATURE)
    @ResponseCacheEvict(group = "instance.info.get")
    public ResponseEntity<Void> disableEmail() {
        query.disableEmail();
        return ResponseEntity.noContent().build();
    }

    @AllowAnonymous
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("hello");
    }
}
