package com.syncturtle.common.web.utils;

import java.util.LinkedHashMap;
import java.util.Map;

import com.syncturtle.common.core.enums.AuthErrorCode;
import com.syncturtle.common.core.enums.AuthFlow;

public final class AuthFormValidator {

    @SuppressWarnings("unused")
    private final Map<AuthFlow, Map<String, AuthErrorCode>> registry = buildRegistry();

    private static Map<AuthFlow, Map<String, AuthErrorCode>> buildRegistry() {
        Map<AuthFlow, Map<String, AuthErrorCode>> map = new LinkedHashMap<>();

        Map<String, AuthErrorCode> emailCheck = new LinkedHashMap<>();
        Map<String, AuthErrorCode> instanceAdminSignup = new LinkedHashMap<>();

        // EMAIL_CHECK
        emailCheck.put("email:NotBlank", AuthErrorCode.EMAIL_REQUIRED);
        emailCheck.put("email:Email", AuthErrorCode.EMAIL_REQUIRED);

        // INSTANCE_ADMIN_SIGNUP
        instanceAdminSignup.put("firstName:NotBlank", AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME);
        instanceAdminSignup.put("email:Email", AuthErrorCode.INVALID_ADMIN_EMAIL);
        instanceAdminSignup.put("email:NotBlank", AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME);
        instanceAdminSignup.put("password:Size", AuthErrorCode.INVALID_ADMIN_PASSWORD);
        instanceAdminSignup.put("password:NotBlank", AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME);

        map.put(AuthFlow.EMAIL_CHECK, emailCheck);
        map.put(AuthFlow.INSTANCE_ADMIN_SIGNUP, instanceAdminSignup);

        return map;
    }

}
