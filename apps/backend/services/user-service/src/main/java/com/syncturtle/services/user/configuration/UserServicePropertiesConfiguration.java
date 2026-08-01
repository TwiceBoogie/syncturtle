package com.syncturtle.services.user.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.services.user.configuration.property.AuthProperties;
import com.syncturtle.services.user.configuration.property.PassportProperties;

@Configuration
@EnableConfigurationProperties({
        PassportProperties.class,
        AuthProperties.class
})
public class UserServicePropertiesConfiguration {

}
