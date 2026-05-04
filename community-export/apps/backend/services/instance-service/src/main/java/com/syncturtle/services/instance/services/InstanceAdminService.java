package com.syncturtle.services.instance.services;

import java.util.List;

import com.syncturtle.services.instance.dto.request.InstanceAdminSigninForm;
import com.syncturtle.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.services.instance.dto.response.InstanceAdminAuthResponse;
import com.syncturtle.services.instance.dto.response.InstanceAdminResponse;

public interface InstanceAdminService {
    InstanceAdminAuthResponse instanceAdminSignup(InstanceAdminSignupForm form);

    InstanceAdminAuthResponse instanceAdminSignin(InstanceAdminSigninForm form);

    List<InstanceAdminResponse> getInstanceAdmins();
}
