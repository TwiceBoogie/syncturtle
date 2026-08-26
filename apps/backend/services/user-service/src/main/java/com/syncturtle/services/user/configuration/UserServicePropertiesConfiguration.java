package com.syncturtle.services.user.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.services.user.configuration.property.AdminSessionHandoffProperties;
import com.syncturtle.services.user.configuration.property.AuthProperties;
import com.syncturtle.services.user.configuration.property.PassportProperties;
import com.syncturtle.services.user.configuration.property.RefreshSessionClientBindingProperties;
import com.syncturtle.services.user.configuration.property.RefreshSessionLifecycleProperties;
import com.syncturtle.services.user.configuration.property.RefreshSessionSuccessorEnvelopeProperties;

@Configuration
@EnableConfigurationProperties({
        AdminSessionHandoffProperties.class,
        PassportProperties.class,
        AuthProperties.class,
        RefreshSessionLifecycleProperties.class,
        RefreshSessionSuccessorEnvelopeProperties.class,
        RefreshSessionClientBindingProperties.class
})
public class UserServicePropertiesConfiguration {

}
