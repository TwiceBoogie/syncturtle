package com.syncturtle.services.user.controller.internal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.contracts.instance.admin.AdminSigninResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSignupResponse;
import com.syncturtle.common.contracts.auth.session.AdminSessionHandoffResponse;
import com.syncturtle.common.contracts.auth.session.CreateInstanceAdminSessionHandoffRequest;
import com.syncturtle.common.contracts.instance.admin.AdminSigninRequest;
import com.syncturtle.common.contracts.instance.admin.AdminSignupRequest;
import com.syncturtle.common.core.endpoint.EndpointPaths;
import com.syncturtle.services.user.service.UserAdministrationInternalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointPaths.INTERNAL_V1_USERS)
public class UserAdministrationInternalController {

    private final UserAdministrationInternalService userAdminService;

    @PostMapping(EndpointPaths.ADMINS_SIGN__UP)
    public AdminSignupResponse adminSignup(@RequestBody AdminSignupRequest request) {
        return userAdminService.adminSignup(request);
    }

    @PostMapping(EndpointPaths.ADMINS_SIGN__IN)
    public AdminSigninResponse adminSignin(@RequestBody AdminSigninRequest request) {
        return userAdminService.adminSignin(request);
    }

    @PostMapping("/admins/session-handoffs")
    public AdminSessionHandoffResponse createInstanceAdminSessionhandoff(
            @RequestBody CreateInstanceAdminSessionHandoffRequest request) {
        return userAdminService.createInstanceAdminSessionHandoff(request);
    }

}
