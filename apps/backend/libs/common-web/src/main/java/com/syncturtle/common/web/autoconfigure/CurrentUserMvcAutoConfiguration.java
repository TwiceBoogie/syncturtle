package com.syncturtle.common.web.autoconfigure;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.common.web.mvc.CurrentUserArgumentResolver;

@AutoConfiguration
@ConditionalOnClass({
        RequestUserContext.class,
        CurrentUserMvcAutoConfiguration.class,
        WebMvcConfigurer.class
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CurrentUserMvcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CurrentUserArgumentResolver currentUserArgumentResolver(RequestUserContext requestUserContext) {
        return new CurrentUserArgumentResolver(requestUserContext);
    }

    @Bean
    @ConditionalOnMissingBean(name = "currentUserArgumentResolverConfigurer")
    WebMvcConfigurer currentUserArgumentResolverConfigurer(
            CurrentUserArgumentResolver currentUserArgumentResolver) {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(
                    List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(currentUserArgumentResolver);
            }
        };
    }

}
