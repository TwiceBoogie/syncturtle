package com.syncturtle.services.user.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.services.user.configuration.property.PassportProperties;
import com.syncturtle.services.user.configuration.property.RefreshSessionClientBindingProperties;

@Configuration
@EnableConfigurationProperties({
        PassportProperties.class,
        RefreshSessionClientBindingProperties.class
})
public class UserServicePropertiesConfiguration {

}
