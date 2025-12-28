package com.syncturtle.common.spring.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ RequestInterceptor.class, RequestContextHolder.class, ServletRequestAttributes.class,
        HttpServletRequest.class })
public class FeignHeaderPropagationAutoConfiguration {

    private static final String HDR_USER_ID = "X-Auth-User-Id";
    private static final String HDR_TENAT = "X-Tenant-Id";
    private static final String HDR_REQ_ID = "X-Request-Id";

    @Bean
    @ConditionalOnMissingBean
    RequestInterceptor syncturtleHeaderPropagationInterceptor() {
        return template -> {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (!(attrs instanceof ServletRequestAttributes sra)) {
                // not in an HTTP request (scheduled job)
                return;
            }

            HttpServletRequest req = sra.getRequest();
            copyIfPresent(req, template, HDR_USER_ID);
            copyIfPresent(req, template, HDR_TENAT);
            copyIfPresent(req, template, HDR_REQ_ID);
        };
    }

    private static void copyIfPresent(HttpServletRequest request, RequestTemplate template, String headerName) {
        String value = request.getHeader(headerName);
        if (value != null && !value.isBlank()) {
            template.header(headerName, value);
        }
    }
}
