package com.syncturtle.common.web.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.syncturtle.common.web.properties.PublicUrlProperties;
import com.syncturtle.common.web.url.PublicUrlResolver;

@AutoConfiguration
@EnableConfigurationProperties(PublicUrlProperties.class)
public class PublicUrlAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    PublicUrlResolver publicUrlResolver(PublicUrlProperties properties) {
        return new PublicUrlResolver(properties);
    }

}
