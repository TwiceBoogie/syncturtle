package com.syncturtle.services.user.services;

import com.syncturtle.common.contracts.instance.admin.AdminSigninResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSignupResponse;
import com.syncturtle.common.contracts.auth.session.IssueInstanceAdminSessionRequest;
import com.syncturtle.common.contracts.auth.session.IssueSessionResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSigninRequest;
import com.syncturtle.common.contracts.instance.admin.AdminSignupRequest;

public interface UserAdminService {
    AdminSignupResponse adminSignup(AdminSignupRequest request);

    AdminSigninResponse adminSignin(AdminSigninRequest request);

    IssueSessionResponse issueInstanceAdminSession(IssueInstanceAdminSessionRequest request);
}
