package com.syncturtle.common.spring.autoconfigure.http.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.web.client.FeignAuthErrorDecoder;
import com.syncturtle.common.web.client.FeignHeaderPropagation;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ RequestInterceptor.class, RequestContextHolder.class, ServletRequestAttributes.class,
        HttpServletRequest.class, FeignClient.class })
public class FeignClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    FeignHeaderPropagation feignHeaderPropagation() {
        return new FeignHeaderPropagation();
    }

    @Bean
    @ConditionalOnMissingBean(name = "syncturtleHeaderPropagationInterceptor")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass({ RequestInterceptor.class, HttpServletRequest.class })
    RequestInterceptor syncturtleHeaderPropagationInterceptor(FeignHeaderPropagation propagation) {
        return propagation.asRequestInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean(ErrorDecoder.class)
    @ConditionalOnClass({ ErrorDecoder.class, ObjectMapper.class })
    ErrorDecoder syncturtleFeignAuthErrorDecoder(ObjectMapper mapper) {
        return new FeignAuthErrorDecoder(mapper);
    }

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
