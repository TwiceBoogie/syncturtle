package com.syncturtle.services.instance.service;

import com.syncturtle.services.instance.dto.request.InstanceAdminSigninForm;
import com.syncturtle.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.services.instance.dto.response.InstanceAdminAuthResponse;

public interface InstanceAdminAuthenticationService {
    InstanceAdminAuthResponse instanceAdminSignup(InstanceAdminSignupForm form);

    InstanceAdminAuthResponse instanceAdminSignin(InstanceAdminSigninForm form);
}
