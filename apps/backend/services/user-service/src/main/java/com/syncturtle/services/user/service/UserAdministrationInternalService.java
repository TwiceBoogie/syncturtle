package com.syncturtle.services.user.service;

import com.syncturtle.common.contracts.instance.admin.AdminSigninResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSignupResponse;
import com.syncturtle.common.contracts.auth.session.AdminSessionHandoffResponse;
import com.syncturtle.common.contracts.auth.session.CreateInstanceAdminSessionHandoffRequest;
import com.syncturtle.common.contracts.instance.admin.AdminSigninRequest;
import com.syncturtle.common.contracts.instance.admin.AdminSignupRequest;

public interface UserAdministrationInternalService {
    AdminSignupResponse adminSignup(AdminSignupRequest request);

    AdminSigninResponse adminSignin(AdminSigninRequest request);

    AdminSessionHandoffResponse createInstanceAdminSessionHandoff(CreateInstanceAdminSessionHandoffRequest request);
}
