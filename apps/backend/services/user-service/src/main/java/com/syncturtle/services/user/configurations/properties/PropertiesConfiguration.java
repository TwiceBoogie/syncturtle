package com.syncturtle.services.user.configurations.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        PassportProperties.class,
        AuthProperties.class
})
public class PropertiesConfiguration {

}
