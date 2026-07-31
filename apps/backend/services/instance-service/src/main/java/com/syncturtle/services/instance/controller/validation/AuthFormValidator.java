package com.syncturtle.services.instance.controller.validation;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.flow.AuthFlow;

@Component
public final class AuthFormValidator {

    private final Map<AuthFlow, Map<String, AuthErrorCode>> registry = buildRegistry();

    public Optional<AuthException> validate(
            AuthFlow flow,
            BindingResult bindingResult,
            Map<String, Object> safePayload) {
        if (!bindingResult.hasErrors()) {
            return Optional.empty();
        }

        FieldError error = bindingResult.getFieldError();
        AuthErrorCode code = resolve(flow, error).orElse(flow.getDefaultError());

        return Optional.of(AuthException.of(code, safePayload));
    }

    public void throwIfInvalid(AuthFlow flow, BindingResult bindingResult, Map<String, Object> safePayload) {
        validate(flow, bindingResult, safePayload)
                .ifPresent(exception -> {
                    throw exception;
                });
    }

    private Optional<AuthErrorCode> resolve(AuthFlow flow, FieldError error) {
        if (error == null) {
            return Optional.empty();
        }

        Map<String, AuthErrorCode> flowRules = registry.get(flow);
        if (flowRules == null) {
            return Optional.empty();
        }

        return constraintNames(error)
                .stream()
                .map(constraint -> error.getField() + ":" + constraint)
                .map(flowRules::get)
                .filter(Objects::nonNull)
                .findFirst();
    }

    private static List<String> constraintNames(FieldError error) {
        if (error.getCodes() == null) {
            return List.of();
        }

        return Arrays.stream(error.getCodes())
                .map(AuthFormValidator::lastSegment)
                .distinct()
                .toList();
    }

    private static String lastSegment(String code) {
        int index = code.lastIndexOf('.');
        return index == -1 ? code : code.substring(index + 1);
    }

    private static Map<AuthFlow, Map<String, AuthErrorCode>> buildRegistry() {
        Map<AuthFlow, Map<String, AuthErrorCode>> map = new LinkedHashMap<>();

        Map<String, AuthErrorCode> adminSignup = new LinkedHashMap<>();
        adminSignup.put("email:NotBlank", AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME);
        adminSignup.put("password:NotBlank", AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME);
        adminSignup.put("firstName:NotBlank", AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD_FIRST_NAME);
        adminSignup.put("email:Email", AuthErrorCode.INVALID_ADMIN_EMAIL);
        adminSignup.put("password:Size", AuthErrorCode.INVALID_ADMIN_PASSWORD);
        adminSignup.put("email:Email", AuthErrorCode.INVALID_ADMIN_EMAIL);
        adminSignup.put("password:Size", AuthErrorCode.INVALID_ADMIN_PASSWORD);
        map.put(AuthFlow.INSTANCE_ADMIN_SIGNUP, adminSignup);

        Map<String, AuthErrorCode> adminSignin = new LinkedHashMap<>();
        adminSignin.put("email:NotBlank", AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD);
        adminSignin.put("password:NotBlank", AuthErrorCode.REQUIRED_ADMIN_EMAIL_PASSWORD);
        adminSignin.put("email:Email", AuthErrorCode.INVALID_ADMIN_EMAIL);
        map.put(AuthFlow.INSTANCE_ADMIN_SIGNIN, adminSignin);

        return map;
    }
}
