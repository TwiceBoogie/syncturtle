package com.syncturtle.platform.services.user.controllers.internal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.web.dto.request.AdminSignupInternalRequest;
import com.syncturtle.common.web.dto.response.AdminSignupInternalResponse;
import com.syncturtle.platform.services.user.services.UserAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @PostMapping("/admins/sign-up")
    public AdminSignupInternalResponse adminSignup(@RequestBody AdminSignupInternalRequest request) {
        return userAdminService.adminSignup(request);
    }

}
