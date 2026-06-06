package com.syncturtle.services.user.service.authz;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationRequest;
import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationResponse;
import com.syncturtle.services.user.client.InstanceClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InstanceAuthorizationResolver {

    private final InstanceClient instanceClient;

    public InstanceAuthorizationSnapshot resolve(UUID userId, UUID instanceId) {
        Assert.notNull(userId, "userId is required");
        Assert.notNull(instanceId, "instanceId is required");

        ResolveInstanceAuthorizationResponse response = instanceClient.resolveAuthz(
                new ResolveInstanceAuthorizationRequest(userId, instanceId));

        Assert.notNull(response, "authorization response is required");

        if (response.isInstanceAdmin()) {
            return InstanceAuthorizationSnapshot.instanceAdmin(response.getAdminSessionVersion());
        }

        return InstanceAuthorizationSnapshot.member();
    }

}
