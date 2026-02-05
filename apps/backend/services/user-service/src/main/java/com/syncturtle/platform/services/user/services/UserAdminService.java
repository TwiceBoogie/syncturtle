package com.syncturtle.platform.services.user.services;

import com.syncturtle.common.web.dto.request.AdminSigninInternalRequest;
import com.syncturtle.common.web.dto.request.AdminSignupInternalRequest;
import com.syncturtle.common.web.dto.response.AdminSigninInternalResponse;
import com.syncturtle.common.web.dto.response.AdminSignupInternalResponse;

public interface UserAdminService {
    AdminSignupInternalResponse adminSignup(AdminSignupInternalRequest request);

    AdminSigninInternalResponse adminSignin(AdminSigninInternalRequest request);
}
