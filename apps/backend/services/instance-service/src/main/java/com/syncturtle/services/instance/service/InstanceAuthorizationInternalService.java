package com.syncturtle.services.instance.service;

import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationRequest;
import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationResponse;

public interface InstanceAuthorizationInternalService {
    ResolveInstanceAuthorizationResponse resolveInstanceAuthz(ResolveInstanceAuthorizationRequest request);
}
