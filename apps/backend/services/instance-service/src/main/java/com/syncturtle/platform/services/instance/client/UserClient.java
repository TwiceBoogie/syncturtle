package com.syncturtle.platform.services.instance.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.syncturtle.common.core.constants.FeignConstants;
import com.syncturtle.common.web.dto.request.AdminSigninInternalRequest;
import com.syncturtle.common.web.dto.request.AdminSignupInternalRequest;
import com.syncturtle.common.web.dto.response.AdminSigninInternalResponse;
import com.syncturtle.common.web.dto.response.AdminSignupInternalResponse;

@FeignClient(value = FeignConstants.USER_SERVICE, path = "/internal/v1/users")
public interface UserClient {

    @PostMapping("/admins/sign-up")
    AdminSignupInternalResponse adminSignupPost(@RequestBody AdminSignupInternalRequest request);

    @PostMapping("/admins/sign-in")
    AdminSigninInternalResponse adminSigninPost(@RequestBody AdminSigninInternalRequest request);

}
