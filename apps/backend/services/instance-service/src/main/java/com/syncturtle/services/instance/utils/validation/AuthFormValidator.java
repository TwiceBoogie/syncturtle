package com.syncturtle.services.instance.utils.validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.flow.AuthFlow;
import com.syncturtle.services.instance.dto.request.InstanceAdminSignupForm;

@Component
public final class AuthFormValidator {

    private final Map<AuthFlow, Map<String, AuthErrorCode>> registry = buildRegistry();

    public void throwIfInvalid(AuthFlow flow, BindingResult bindingResult, Object form) {
        if (!bindingResult.hasErrors()) {
            return;
        }

        FieldError first = bindingResult.getFieldErrors().stream().findFirst().orElse(null);
        AuthErrorCode code = resolve(flow, first).orElse(flow.getDefaultError());

        Map<String, Object> payload = buildSafePayload(flow, form);

        if (first != null) {
            payload.put("field", first.getField());
        }

        throw new AuthException(code, payload);
    }

    private Optional<AuthErrorCode> resolve(AuthFlow flow, FieldError error) {
        if (error == null) {
            return Optional.empty();
        }

        String constraint = error.getCode();
        String key = error.getField() + ":" + constraint;

        Map<String, AuthErrorCode> flowRules = registry.get(flow);
        if (flowRules == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(flowRules.get(key));
    }

    private Map<String, Object> buildSafePayload(AuthFlow flow, Object form) {
        Map<String, Object> payload = new LinkedHashMap<>();

        if (flow == AuthFlow.INSTANCE_ADMIN_SIGNUP && form instanceof InstanceAdminSignupForm f) {
            payload.put("email", f.getEmail());
            payload.put("firstName", f.getFirstName());
            payload.put("lastName", f.getLastName());
            payload.put("companyName", f.getCompanyName());
            payload.put("isTelemetryEnabled", f.getTelemetryEnabled());
        }

        return payload;
    }

    private static Map<AuthFlow, Map<String, AuthErrorCode>> buildRegistry() {
        Map<AuthFlow, Map<String, AuthErrorCode>> map = new LinkedHashMap<>();

        Map<String, AuthErrorCode> signup = new LinkedHashMap<>();

        // REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME
        signup.put("email:NotBlank", AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME);
        signup.put("password:NotBlank", AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME);
        signup.put("firstName:NotBlank", AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME);

        // INVALID_ADMIN_EMAIL
        signup.put("email:Email", AuthErrorCode.INVALID_ADMIN_EMAIL);

        signup.put("password:Size", AuthErrorCode.INVALID_ADMIN_PASSWORD);

        map.put(AuthFlow.INSTANCE_ADMIN_SIGNUP, signup);

        return map;
    }
}
