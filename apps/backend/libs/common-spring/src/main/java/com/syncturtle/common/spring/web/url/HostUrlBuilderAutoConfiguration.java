package com.syncturtle.common.spring.web.url;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.spring.properties.HostRoutingProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HostRoutingProperties.class)
public class HostUrlBuilderAutoConfiguration {

    @Bean
    HostUrlBuilder hostUrlBuilder(HostRoutingProperties props) {
        return new HostUrlBuilder(props);
    }

    @Bean
    @ConditionalOnWebApplication(type = Type.SERVLET)
    ServletHostUrlBuilder servletHostUrlBuilder(HostUrlBuilder core) {
        return new ServletHostUrlBuilder(core);
    }

    @Bean
    @ConditionalOnWebApplication(type = Type.REACTIVE)
    ReactiveHostUrlBuilder reactiveHostUrlBuilder(HostUrlBuilder core) {
        return new ReactiveHostUrlBuilder(core);
    }
}
