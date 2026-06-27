package com.syncturtle.services.instance.support.fixture;

import java.util.UUID;

import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationRequest;
import com.syncturtle.services.instance.dto.request.InstanceAdminSigninForm;
import com.syncturtle.services.instance.dto.request.InstanceAdminSignupForm;

public final class RequestFixtures {

    private RequestFixtures() {
    }

    public static InstanceAdminSignupForm validSignupForm() {
        InstanceAdminSignupForm form = new InstanceAdminSignupForm();
        form.setFirstName("Luna");
        form.setLastName("Snow");
        form.setEmail("lunasnow@marvel.com");
        form.setPassword("secret-password");
        form.setCompanyName("Syncturtle");
        form.setTelemetryEnabled("true");

        return form;
    }

    public static InstanceAdminSigninForm validSigninForm() {
        InstanceAdminSigninForm form = new InstanceAdminSigninForm();
        form.setEmail("lunasnow@marvel.com");
        form.setPassword("secret-password");
        return form;
    }

    public static ResolveInstanceAuthorizationRequest noInstanceId() {
        return new ResolveInstanceAuthorizationRequest(UUID.randomUUID(), null);
    }

    public static ResolveInstanceAuthorizationRequest noUserId() {
        return new ResolveInstanceAuthorizationRequest(null, UUID.randomUUID());
    }

    public static ResolveInstanceAuthorizationRequest validResolveInstanceAuthorizationRequest() {
        return new ResolveInstanceAuthorizationRequest(UUID.randomUUID(), UUID.randomUUID());
    }

}
