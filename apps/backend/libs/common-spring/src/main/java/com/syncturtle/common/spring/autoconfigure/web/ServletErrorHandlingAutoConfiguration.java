package com.syncturtle.common.spring.autoconfigure.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.syncturtle.common.spring.properties.ErrorResponseProperties;
import com.syncturtle.common.spring.web.error.ApiErrorResponseFactory;
import com.syncturtle.common.spring.web.error.ServletGlobalExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@AutoConfiguration
@EnableConfigurationProperties(ErrorResponseProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ HttpServletRequest.class, ServletGlobalExceptionHandler.class, ApiErrorResponseFactory.class })
public class ServletErrorHandlingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ApiErrorResponseFactory apiErrorResponseFactory(
            ErrorResponseProperties properties,
            Environment environment) {
        return new ApiErrorResponseFactory(properties, environment);
    }

    @Bean
    @ConditionalOnMissingBean
    ServletGlobalExceptionHandler servletGlobalExceptionHandler(
            ApiErrorResponseFactory responseFactory) {
        return new ServletGlobalExceptionHandler(responseFactory);
    }

}
