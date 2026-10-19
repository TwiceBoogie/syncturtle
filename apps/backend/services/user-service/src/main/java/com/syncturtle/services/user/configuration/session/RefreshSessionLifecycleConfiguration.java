package com.syncturtle.services.user.configuration.session;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.core.security.token.TokenHasher;
import com.syncturtle.services.user.configuration.property.RefreshSessionClientBindingProperties;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionClientFingerprintFactory;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionStore;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionRedisKeys;
import com.syncturtle.services.user.service.collaborator.session.UserAuthVersionStore;

import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class RefreshSessionLifecycleConfiguration {

    @Bean
    RefreshSessionRedisKeys refreshSessionRedisKeys(RedisKeyBuilder redisKeyBuilder) {
        return new RefreshSessionRedisKeys(redisKeyBuilder);
    }

    @Bean
    RefreshSessionClientFingerprintFactory refreshSessionClientFingerprintFactory(
            RefreshSessionClientBindingProperties properties) {
        return new RefreshSessionClientFingerprintFactory(properties);
    }

    @Bean
    RefreshSessionStore refreshSessionStore(
            StringRedisTemplate redis,
            RefreshSessionRedisKeys keys,
            SecureTokenGenerator tokenGenerator,
            TokenHasher tokenHasher,
            UserAuthVersionStore versionStore,
            JsonMapper jsonMapper,
            Clock clock) {
        return new RefreshSessionStore(
                redis,
                keys,
                tokenGenerator,
                tokenHasher,
                versionStore,
                jsonMapper,
                clock);
    }

}
