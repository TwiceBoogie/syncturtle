package com.syncturtle.platform.services.instance.controllers.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.spring.cache.response.ResponseCache;
import com.syncturtle.common.spring.cache.response.ResponseCacheEvict;
import com.syncturtle.common.spring.security.authz.AllowAnonymous;
import com.syncturtle.common.spring.security.authz.RequireInstanceAdmin;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/instances")
@RequireInstanceAdmin(minRole = 15)
public class InstanceController {

    @GetMapping
    @AllowAnonymous
    @ResponseCache(group = "instance.info.get", ttlSeconds = 60 * 60 * 2, perUser = false, perWorkspace = false)
    public ResponseEntity<?> getInstanceInfo() {
        return null;
    }

    @PatchMapping
    @ResponseCacheEvict(group = "instance.info.get", beforeInvocation = true)
    public ResponseEntity<?> updateInstanceInfo() {
        return null;
    }

    @AllowAnonymous
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("hello");
    }
}
