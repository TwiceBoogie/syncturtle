package com.syncturtle.common.web.client;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.syncturtle.common.core.header.GatewayHeaders;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;

public class FeignHeaderPropagation {

    public RequestInterceptor asRequestInterceptor() {
        return template -> {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (!(attrs instanceof ServletRequestAttributes sra)) {
                // not in an HTTP request (scheduled job)
                return;
            }

            HttpServletRequest req = sra.getRequest();

            // identity
            copyIfPresent(req, template, GatewayHeaders.HDR_AUTH_USER_ID);
            copyIfPresent(req, template, GatewayHeaders.HDR_AUTH_WORKSPACE_ID);

            // tracing
            copyIfPresent(req, template, GatewayHeaders.HDR_REQUEST_ID);
            copyIfPresent(req, template, GatewayHeaders.HDR_CORRELATION_ID);

            // client metadata
            copyIfPresent(req, template, GatewayHeaders.HDR_CLIENT_IP);
            copyIfPresent(req, template, GatewayHeaders.HDR_CLIENT_UA);
        };
    }

    private static void copyIfPresent(HttpServletRequest request, RequestTemplate template, String headerName) {
        String value = request.getHeader(headerName);
        if (value != null && !value.isBlank()) {
            template.header(headerName, value);
        }
    }

}
