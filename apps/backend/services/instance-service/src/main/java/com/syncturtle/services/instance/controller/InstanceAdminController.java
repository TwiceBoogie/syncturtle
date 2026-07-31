package com.syncturtle.services.instance.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.cache.response.annotation.CacheResponse;
import com.syncturtle.common.cache.response.annotation.InvalidateCache;
import com.syncturtle.common.core.endpoint.EndpointPaths;
import com.syncturtle.common.security.annotation.AllowAnonymous;
import com.syncturtle.common.security.annotation.RequireInstancePermission;
import com.syncturtle.common.web.annotation.CurrentUser;
import com.syncturtle.services.instance.dto.request.InstanceAdminCreateRequest;
import com.syncturtle.services.instance.dto.response.InstanceAdminMeResponse;
import com.syncturtle.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.services.instance.dto.response.InstanceAdminSessionResponse;
import com.syncturtle.services.instance.service.InstanceAdminService;
import com.syncturtle.services.instance.type.InstanceAdminRoleCodes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointPaths.API_INSTANCES)
public class InstanceAdminController {

    private final InstanceAdminService instanceAdminService;

    @GetMapping(EndpointPaths.ADMINS)
    @RequireInstancePermission(minRole = InstanceAdminRoleCodes.ADMIN)
    @CacheResponse(group = "instance.admin.list.v1", ttlSeconds = 60 * 60 * 2, perUser = false, perWorkspace = false)
    public ResponseEntity<List<InstanceAdminResponse>> getInstanceAdmins() {
        return ResponseEntity.ok(instanceAdminService.getAdmins());
    }

    @PostMapping(EndpointPaths.ADMINS)
    @InvalidateCache(group = "instance.public-info.v1")
    @InvalidateCache(group = "instance.admin.list.v1")
    @RequireInstancePermission(minRole = InstanceAdminRoleCodes.ADMIN)
    public ResponseEntity<InstanceAdminResponse> createAdmin(@Valid @RequestBody InstanceAdminCreateRequest request) {
        InstanceAdminResponse response = instanceAdminService.createAdmin(request.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping(EndpointPaths.ADMINS_ID)
    @InvalidateCache(group = "instance.public-info.v1")
    @InvalidateCache(group = "instance.admin.list.v1")
    @RequireInstancePermission(minRole = InstanceAdminRoleCodes.ADMIN)
    public ResponseEntity<Void> deleteAdmin(@CurrentUser UUID currentUserId, @PathVariable UUID instanceAdminId) {
        instanceAdminService.deleteAdmin(currentUserId, instanceAdminId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(EndpointPaths.ADMINS_ME)
    @RequireInstancePermission(minRole = InstanceAdminRoleCodes.ADMIN)
    public ResponseEntity<InstanceAdminMeResponse> getCurrentAdmin(@CurrentUser UUID currentUserId) {
        return ResponseEntity.ok(instanceAdminService.getCurrentAdmin(currentUserId));
    }

    @AllowAnonymous
    @GetMapping(EndpointPaths.ADMINS_SESSION)
    public ResponseEntity<InstanceAdminSessionResponse> session(@CurrentUser(required = false) UUID currentUserId) {
        return ResponseEntity.ok(instanceAdminService.getSession(currentUserId));
    }

}
