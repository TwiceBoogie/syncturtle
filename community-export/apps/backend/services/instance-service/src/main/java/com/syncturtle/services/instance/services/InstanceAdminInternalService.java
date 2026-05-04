package com.syncturtle.services.instance.services;

import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationRequest;
import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationResponse;

public interface InstanceAdminInternalService {
    ResolveInstanceAuthorizationResponse resolveInstanceAuthz(ResolveInstanceAuthorizationRequest request);
}
