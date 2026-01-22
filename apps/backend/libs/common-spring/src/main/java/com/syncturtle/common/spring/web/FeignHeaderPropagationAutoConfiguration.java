package com.syncturtle.common.spring.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.syncturtle.common.core.constants.GatewayHeaderNames;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ RequestInterceptor.class, RequestContextHolder.class, ServletRequestAttributes.class,
        HttpServletRequest.class })
public class FeignHeaderPropagationAutoConfiguration {

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

            // identity
            copyIfPresent(req, template, GatewayHeaderNames.HDR_AUTH_USER_ID);
            copyIfPresent(req, template, GatewayHeaderNames.HDR_AUTH_WORKSPACE_ID);

            // tracing
            copyIfPresent(req, template, GatewayHeaderNames.HDR_REQUEST_ID);
            copyIfPresent(req, template, GatewayHeaderNames.HDR_CORRELATION_ID);

            // client metadata
            copyIfPresent(req, template, GatewayHeaderNames.HDR_CLIENT_IP);
            copyIfPresent(req, template, GatewayHeaderNames.HDR_CLIENT_UA);
        };
    }

    private static void copyIfPresent(HttpServletRequest request, RequestTemplate template, String headerName) {
        String value = request.getHeader(headerName);
        if (value != null && !value.isBlank()) {
            template.header(headerName, value);
        }
    }
}
