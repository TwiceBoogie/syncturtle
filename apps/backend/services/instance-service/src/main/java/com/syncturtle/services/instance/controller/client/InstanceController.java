package com.syncturtle.services.instance.controller.client;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.cache.response.annotation.CacheResponse;
import com.syncturtle.common.cache.response.annotation.InvalidateCache;
import com.syncturtle.common.core.endpoint.EndpointPaths;
import com.syncturtle.common.security.annotation.AllowAnonymous;
import com.syncturtle.common.security.annotation.RequireInstancePermission;
import com.syncturtle.services.instance.dto.request.InstanceUpdateRequest;
import com.syncturtle.services.instance.dto.response.InstanceResponse;
import com.syncturtle.services.instance.dto.response.InstanceSetupResponse;
import com.syncturtle.services.instance.service.InstanceService;
import com.syncturtle.services.instance.type.InstanceAdminRoleCodes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointPaths.API_INSTANCES)
@RequireInstancePermission(minRole = InstanceAdminRoleCodes.USER)
public class InstanceController {

    private final InstanceService service;

    @GetMapping
    @AllowAnonymous
    @CacheResponse(group = "instance.public-info.v1", ttlSeconds = 60 * 60 * 2, perUser = false, perWorkspace = false)
    public ResponseEntity<InstanceSetupResponse> getInstanceSetupInfo() {
        return ResponseEntity.ok(service.getPublicInstance());
    }

    @PatchMapping
    @InvalidateCache(group = "instance.public-info.v1")
    public ResponseEntity<InstanceResponse> updateInstance(@Valid @RequestBody InstanceUpdateRequest request) {
        return ResponseEntity.ok(service.instanceUpdate(request));
    }

    // @DeleteMapping(EndpointPaths.CONFIGURATIONS_DISABLE_EMAIL_FEATURE)
    // @ResponseCacheEvict(group = "instance.info.get")
    // @ResponseCacheEvict(group = "instance.config.get")
    // public ResponseEntity<Void> disableEmail() {
    // query.disableEmail();
    // return ResponseEntity.noContent().build();
    // }

    @AllowAnonymous
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("hello");
    }
}
