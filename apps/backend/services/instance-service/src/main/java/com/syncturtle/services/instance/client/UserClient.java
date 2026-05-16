package com.syncturtle.services.instance.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.syncturtle.common.contracts.instance.admin.AdminSigninResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSignupResponse;
import com.syncturtle.common.contracts.auth.session.IssueInstanceAdminSessionRequest;
import com.syncturtle.common.contracts.auth.session.IssueSessionResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSigninRequest;
import com.syncturtle.common.contracts.instance.admin.AdminSignupRequest;
import com.syncturtle.common.core.service.ServiceClientNames;

@FeignClient(value = ServiceClientNames.USER_SERVICE, contextId = "instanceUserClient", url = "${app.services.user-service.base-url}", path = "/internal/v1/users")
public interface UserClient {

    @PostMapping("/admins/sign-up")
    AdminSignupResponse adminSignupPost(@RequestBody AdminSignupRequest request);

    @PostMapping("/admins/sign-in")
    AdminSigninResponse adminSigninPost(@RequestBody AdminSigninRequest request);

    @PostMapping("/admins/sessions")
    IssueSessionResponse issueInstanceAdminSessionPost(
            @RequestBody IssueInstanceAdminSessionRequest request);

}
