package com.syncturtle.services.instance.controllers.client;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.core.endpoint.EndpointPaths;
import com.syncturtle.common.spring.cache.response.ResponseCache;
import com.syncturtle.common.spring.cache.response.ResponseCacheEvict;
import com.syncturtle.common.spring.security.authz.AllowAnonymous;
import com.syncturtle.common.spring.security.authz.RequireInstanceAdmin;
import com.syncturtle.services.instance.application.query.InstanceQueryHandler;
import com.syncturtle.services.instance.dto.request.InstanceRequest;
import com.syncturtle.services.instance.dto.response.InstanceConfigurationResponse;
import com.syncturtle.services.instance.dto.response.InstanceInfo;
import com.syncturtle.services.instance.dto.response.InstanceResponse;
import com.syncturtle.services.instance.dto.response.UserMeResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointPaths.API_INSTANCES)
@RequireInstanceAdmin(minRole = 15)
public class InstanceController {

    private final InstanceQueryHandler query;

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

    @GetMapping(EndpointPaths.CONFIGURATIONS)
    @ResponseCache(group = "instance.config.get", ttlSeconds = 60 * 60 * 2, perUser = false, perWorkspace = false)
    public ResponseEntity<List<InstanceConfigurationResponse>> configurationsAll() {
        return ResponseEntity.ok(query.configurationsAll());
    }

    @PatchMapping(EndpointPaths.CONFIGURATIONS)
    @ResponseCacheEvict(group = "instance.config.get")
    @ResponseCacheEvict(group = "instance.info.get")
    public ResponseEntity<List<InstanceConfigurationResponse>> configurationsUpdate(
            @RequestBody Map<InstanceConfigurationKey, String> request) {
        return ResponseEntity.ok(query.configurationUpdate(request));
    }

    @GetMapping(EndpointPaths.ADMINS_ME)
    public ResponseEntity<UserMeResponse> getInstanceAdminUserMe() {
        return ResponseEntity.ok(query.getInstanceAdminUserMe());
    }

    @DeleteMapping(EndpointPaths.CONFIGURATIONS_DISABLE_EMAIL_FEATURE)
    @ResponseCacheEvict(group = "instance.info.get")
    @ResponseCacheEvict(group = "instance.config.get")
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
