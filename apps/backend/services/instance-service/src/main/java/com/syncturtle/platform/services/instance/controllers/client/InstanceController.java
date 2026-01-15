package com.syncturtle.platform.services.instance.controllers.client;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.spring.cache.response.ResponseCache;
import com.syncturtle.common.spring.cache.response.ResponseCacheEvict;
import com.syncturtle.common.spring.security.authz.AllowAnonymous;
import com.syncturtle.common.spring.security.authz.RequireInstanceAdmin;
import com.syncturtle.platform.services.instance.application.query.InstanceInfoQueryHandler;
import com.syncturtle.platform.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.platform.services.instance.dto.response.InstanceInfo;
import com.syncturtle.platform.services.instance.dto.response.UserMeResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/instances")
@RequireInstanceAdmin(minRole = 15)
public class InstanceController {

    private final InstanceInfoQueryHandler query;

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
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("hello");
    }
}
