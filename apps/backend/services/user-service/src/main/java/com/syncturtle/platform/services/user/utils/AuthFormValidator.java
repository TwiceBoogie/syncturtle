package com.syncturtle.platform.services.user.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import com.syncturtle.common.core.enums.AuthErrorCode;
import com.syncturtle.common.core.enums.AuthFlow;
import com.syncturtle.common.core.exceptions.AuthenticationException;

@Component
public class AuthFormValidator {

    private final Map<AuthFlow, Map<String, AuthErrorCode>> registry = buildRegistry();

    public void throwIfInvalid(AuthFlow flow, BindingResult bindingResult, Object requestOrForm) {
        if (!bindingResult.hasErrors()) {
            return;
        }

        FieldError error = bindingResult.getFieldError();
        AuthErrorCode code = resolve(flow, error).orElse(flow.getDefaultError());

        throw new AuthenticationException(code);
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

    private static Map<AuthFlow, Map<String, AuthErrorCode>> buildRegistry() {
        Map<AuthFlow, Map<String, AuthErrorCode>> map = new LinkedHashMap<>();

        Map<String, AuthErrorCode> emailCheck = new LinkedHashMap<>();

        // EMAIL_CHECK
        emailCheck.put("email:NotBlank", AuthErrorCode.EMAIL_REQUIRED);
        emailCheck.put("email:Email", AuthErrorCode.EMAIL_REQUIRED);

        map.put(AuthFlow.EMAIL_CHECK, emailCheck);

        return map;
    }

}
