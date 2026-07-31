package com.syncturtle.services.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.syncturtle.common.contracts.api.error.ApiErrorResponse;
import com.syncturtle.common.contracts.auth.error.AuthErrorCode;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CsrfFailurePageController {

    @RequestMapping("/csrf-failure")
    public String csrfFailure(HttpServletRequest request, Model model) {
        Object errorAttribute = request.getAttribute("syncturtle.csrf.error");
        Object homeUrlAttribute = request.getAttribute("syncturtle.csrf.homeUrl");

        ApiErrorResponse error = errorAttribute instanceof ApiErrorResponse apiErrorResponse
                ? apiErrorResponse
                : fallbackError();

        String homeUrl = homeUrlAttribute instanceof String value && !value.isBlank()
                ? value
                : "/";

        model.addAttribute("error", error);
        model.addAttribute("homeUrl", homeUrl);

        return "csrf-failure";
    }

    private static ApiErrorResponse fallbackError() {
        return ApiErrorResponse.builder()
                .code(AuthErrorCode.INVALID_CSRF_TOKEN.getCode())
                .key(AuthErrorCode.INVALID_CSRF_TOKEN.getKey())
                .message(AuthErrorCode.INVALID_CSRF_TOKEN.getPublicMessage())
                .path("/csrf-failure")
                .build();
    }

}
