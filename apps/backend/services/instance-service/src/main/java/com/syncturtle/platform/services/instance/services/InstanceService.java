package com.syncturtle.platform.services.instance.services;

import java.util.List;
import java.util.Optional;

import com.syncturtle.platform.services.instance.dto.internal.InstanceAdminSigninResult;
import com.syncturtle.platform.services.instance.dto.internal.InstanceAdminSignupResult;
import com.syncturtle.platform.services.instance.dto.request.InstanceAdminSigninForm;
import com.syncturtle.platform.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.platform.services.instance.dto.request.InstanceRequest;
import com.syncturtle.platform.services.instance.models.User;
import com.syncturtle.platform.services.instance.payload.InstanceSummary;
import com.syncturtle.platform.services.instance.payload.InstanceSummaryWithConfig;
import com.syncturtle.platform.services.instance.repositories.projections.InstanceAdminProjection;

public interface InstanceService {
    Optional<InstanceSummaryWithConfig> instanceInfoAndConfig();

    Optional<User> getInstanceAdminUserMe();

    List<InstanceAdminProjection> getInstanceAdmins();

    InstanceAdminSignupResult instanceAdminSignup(InstanceAdminSignupForm form);

    InstanceAdminSigninResult instanceAdminSignin(InstanceAdminSigninForm form);

    InstanceSummary instanceUpdate(InstanceRequest request);
}
