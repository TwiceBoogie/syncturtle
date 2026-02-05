package com.syncturtle.platform.services.user.controllers.internal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.core.constants.EndpointConstants;
import com.syncturtle.common.web.dto.request.AdminSigninInternalRequest;
import com.syncturtle.common.web.dto.request.AdminSignupInternalRequest;
import com.syncturtle.common.web.dto.response.AdminSigninInternalResponse;
import com.syncturtle.common.web.dto.response.AdminSignupInternalResponse;
import com.syncturtle.platform.services.user.services.UserAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointConstants.INTERNAL_V1_USERS)
public class UserAdminController {

    private final UserAdminService userAdminService;

    @PostMapping(EndpointConstants.ADMINS_SIGN__UP)
    public AdminSignupInternalResponse adminSignup(@RequestBody AdminSignupInternalRequest request) {
        return userAdminService.adminSignup(request);
    }

    @PostMapping(EndpointConstants.ADMINS_SIGN__IN)
    public AdminSigninInternalResponse adminSignin(@RequestBody AdminSigninInternalRequest request) {
        return userAdminService.adminSignin(request);
    }

}
