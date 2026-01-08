package com.syncturtle.platform.infra.gateway.support;

import java.time.Duration;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.platform.infra.gateway.configurations.properties.SessionGatewayProperties;
import com.syncturtle.platform.infra.gateway.enums.SessionType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RedisSessionStore implements SessionStore {

    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SessionGatewayProperties props;

    @Override
    public Mono<SessionRecord> find(SessionType type, String sessionId) {
        return redis.opsForValue()
                .get(key(type, sessionId))
                .flatMap(json -> {
                    if (json == null) {
                        return Mono.empty();
                    }
                    try {
                        return Mono.just(objectMapper.readValue(json, SessionRecord.class));
                    } catch (Exception e) {
                        // corrupted session payload -> treat as missing
                        return Mono.empty();
                    }
                });
    }

    @Override
    public Mono<Void> save(SessionType type, String sessionId, SessionRecord record, Duration absoluteRemainingTtl) {
        if (absoluteRemainingTtl.isNegative() || absoluteRemainingTtl.isZero()) {
            return delete(type, sessionId);
        }

        try {
            String json = objectMapper.writeValueAsString(record);
            String key = key(type, sessionId);

            // IMPORTANT: SET with TTL so redis always enforces absolute expiry
            return redis.opsForValue()
                    .set(key, json, absoluteRemainingTtl)
                    .then();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Void> delete(SessionType type, String sessionId) {
        return redis.delete(key(type, sessionId)).then();
    }

    private String key(SessionType type, String sessionId) {
        // "st" = syncturtle
        return props.getRedisKeyPrefix() + ":sess:" + type.name().toLowerCase() + ":" + sessionId;
    }

}
