package com.syncturtle.services.user.configuration.session;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.core.security.token.TokenHasher;
import com.syncturtle.services.user.configuration.property.AdminSessionHandoffProperties;
import com.syncturtle.services.user.service.collaborator.session.AdminSessionHandoffRedisKeys;
import com.syncturtle.services.user.service.collaborator.session.AdminSessionHandoffScriptExecutor;
import com.syncturtle.services.user.service.collaborator.session.AdminSessionHandoffSerializer;
import com.syncturtle.services.user.service.collaborator.session.AdminSessionHandoffStore;

import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class AdminSessionHandoffConfiguration {

    @Bean
    AdminSessionHandoffRedisKeys adminSessionHandoffRedisKeys(RedisKeyBuilder keyBuilder) {
        return new AdminSessionHandoffRedisKeys(keyBuilder);
    }

    @Bean
    AdminSessionHandoffSerializer adminSessionHandoffSerializer(JsonMapper jsonMapper) {
        return new AdminSessionHandoffSerializer(jsonMapper);
    }

    @Bean
    AdminSessionHandoffScriptExecutor adminSessionHandoffScriptExecutor(StringRedisTemplate redis) {
        return new AdminSessionHandoffScriptExecutor(redis);
    }

    @Bean
    AdminSessionHandoffStore adminSessionHandoffStore(
            StringRedisTemplate redis,
            AdminSessionHandoffRedisKeys keys,
            SecureTokenGenerator tokenGenerator,
            TokenHasher tokenHasher,
            AdminSessionHandoffSerializer serializer,
            AdminSessionHandoffScriptExecutor scriptExecutor,
            AdminSessionHandoffProperties properties,
            Clock clock) {
        return new AdminSessionHandoffStore(
                redis,
                keys,
                tokenGenerator,
                tokenHasher,
                serializer,
                scriptExecutor,
                properties,
                clock);
    }

}
