package com.syncturtle.services.instance.controller.client;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.cache.response.annotation.CacheResponse;
import com.syncturtle.common.cache.response.annotation.InvalidateCache;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.core.endpoint.EndpointPaths;
import com.syncturtle.common.security.annotation.RequireInstancePermission;
import com.syncturtle.services.instance.dto.response.InstanceConfigurationResponse;
import com.syncturtle.services.instance.service.InstanceConfigurationService;
import com.syncturtle.services.instance.type.InstanceAdminRoleCodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointPaths.API_INSTANCES)
@RequireInstancePermission(minRole = InstanceAdminRoleCodes.ADMIN)
public class InstanceConfigurationController {

    private final InstanceConfigurationService service;

    @GetMapping(EndpointPaths.CONFIGURATIONS)
    @CacheResponse(group = "instance.config.list.v1", ttlSeconds = 60 * 60 * 2, perUser = false, perWorkspace = false)
    public ResponseEntity<List<InstanceConfigurationResponse>> getConfigurations() {
        return ResponseEntity.ok(service.configurationsAll());
    }

    @PatchMapping(EndpointPaths.CONFIGURATIONS)
    @InvalidateCache(group = "instance.public-info.v1")
    @InvalidateCache(group = "instance.config.list.v1")
    public ResponseEntity<List<InstanceConfigurationResponse>> updateConfigurations(
            @RequestBody Map<InstanceConfigurationKey, String> request) {
        return ResponseEntity.ok(service.configurationsUpdate(request));
    }

    @DeleteMapping(EndpointPaths.CONFIGURATIONS_DISABLE_EMAIL_FEATURE)
    @InvalidateCache(group = "instance.public-info.v1")
    @InvalidateCache(group = "instance.config.list.v1")
    public ResponseEntity<Void> disableEmail() {
        service.disableEmail();
        return ResponseEntity.noContent().build();
    }

}
