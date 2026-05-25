package com.syncturtle.common.web.autoconfigure;

import java.util.Optional;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.syncturtle.common.core.actor.CurrentActorProvider;
import com.syncturtle.common.web.context.RequestUserContext;

@AutoConfiguration(after = GatewayContextAutoConfiguration.class)
@ConditionalOnBean(RequestUserContext.class)
public class RequestUserActorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CurrentActorProvider.class)
    CurrentActorProvider requestUserCurrentActorProvider(RequestUserContext requestUserContext) {
        return () -> Optional.ofNullable(requestUserContext.getUserId());
    }

}
