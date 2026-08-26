package com.syncturtle.services.user.configuration.session;

import java.security.SecureRandom;
import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.core.security.token.TokenHasher;
import com.syncturtle.services.user.configuration.property.RefreshSessionClientBindingProperties;
import com.syncturtle.services.user.configuration.property.RefreshSessionLifecycleProperties;
import com.syncturtle.services.user.configuration.property.RefreshSessionSuccessorEnvelopeProperties;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionClientFingerprintFactory;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionFamilySerializer;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionFamilyStore;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionLifecycleScriptExecutor;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionLifetimeDecider;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionRedisKeys;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionSuccessorEnvelopeCipher;

import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class RefreshSessionLifecycleConfiguration {

    @Bean
    RefreshSessionFamilySerializer refreshSessionFamilySerializer(JsonMapper jsonMapper) {
        return new RefreshSessionFamilySerializer(jsonMapper);
    }

    @Bean
    RefreshSessionRedisKeys refreshSessionRedisKeys(RedisKeyBuilder redisKeyBuilder) {
        return new RefreshSessionRedisKeys(redisKeyBuilder);
    }

    @Bean
    RefreshSessionLifetimeDecider refreshSessionLifetimeDecider(RefreshSessionLifecycleProperties properties,
            Clock clock) {
        return new RefreshSessionLifetimeDecider(properties, clock);
    }

    @Bean
    RefreshSessionSuccessorEnvelopeCipher refreshSessionSuccessorEnvelopeCipher(
            RefreshSessionSuccessorEnvelopeProperties properties) {
        return new RefreshSessionSuccessorEnvelopeCipher(properties, new SecureRandom());
    }

    @Bean
    RefreshSessionClientFingerprintFactory refreshSessionClientFingerprintFactory(
            RefreshSessionClientBindingProperties properties) {
        return new RefreshSessionClientFingerprintFactory(properties);
    }

    @Bean
    RefreshSessionLifecycleScriptExecutor refreshSessionLifecycleScriptExecutor(StringRedisTemplate redis) {
        return new RefreshSessionLifecycleScriptExecutor(redis);
    }

    @Bean
    RefreshSessionFamilyStore refreshSessionFamilyStore(
            StringRedisTemplate redis,
            RefreshSessionRedisKeys keys,
            SecureTokenGenerator tokenGenerator,
            TokenHasher tokenHasher,
            RefreshSessionFamilySerializer familySerializer,
            RefreshSessionLifetimeDecider lifetimeDecider,
            RefreshSessionSuccessorEnvelopeCipher envelopeCipher,
            RefreshSessionLifecycleScriptExecutor scriptExecutor,
            RefreshSessionLifecycleProperties properties,
            JsonMapper jsonMapper) {
        return new RefreshSessionFamilyStore(
                redis,
                keys,
                tokenGenerator,
                tokenHasher,
                familySerializer,
                lifetimeDecider,
                envelopeCipher,
                scriptExecutor,
                properties,
                jsonMapper);
    }

}
