package com.syncturtle.platform.tests;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.syncturtle.platform.tests.assertions.HttpAssertions;
import com.syncturtle.platform.tests.clients.GatewayApi;
import com.syncturtle.platform.tests.env.ServiceUrls;
import com.syncturtle.platform.tests.env.SyncturtleEnvironment;

@ExtendWith(SyncturtleEnvironment.class)
public class GatewayActuatorE2E {

    private GatewayApi gateway() {
        return new GatewayApi(ServiceUrls.gateway());
    }

    @Test
    void actuatorHealth_shouldBeUp() throws Exception {
        HttpResponse<String> response = gateway().getActuatorRaw("/health");

        HttpAssertions.assertStatusIn(response.statusCode(), 200);
    }

}
