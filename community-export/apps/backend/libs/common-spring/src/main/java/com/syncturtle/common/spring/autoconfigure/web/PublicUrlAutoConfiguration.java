package com.syncturtle.common.spring.autoconfigure.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.spring.properties.PublicUrlProperties;
import com.syncturtle.common.spring.web.url.PublicUrlBuilder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PublicUrlProperties.class)
public class PublicUrlAutoConfiguration {

    @Bean
    PublicUrlBuilder publicUrlBuilder(PublicUrlProperties properties) {
        return new PublicUrlBuilder(properties);
    }

}
