package com.syncturtle.services.user.service.collaborator.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.syncturtle.common.cache.property.RedisKeyProperties;
import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.common.core.security.token.Base64UrlSecureTokenGenerator;
import com.syncturtle.common.core.security.token.Sha256TokenHasher;
import com.syncturtle.services.user.exception.RefreshSessionException;

import tools.jackson.databind.json.JsonMapper;

class RefreshSessionStoreTest {

    @Test
    void unavailableRedisFailsClosedDuringRefreshLookup() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        RefreshSessionRedisKeys keys = new RefreshSessionRedisKeys(
                new RedisKeyBuilder(new RedisKeyProperties("st:test:refresh-session:")));
        String sessionId = "33333333-3333-3333-3333-333333333333";
        when(values.get(keys.session(sessionId))).thenThrow(new DataAccessResourceFailureException("unavailable"));
        RefreshSessionStore store = new RefreshSessionStore(redis, keys, new Base64UrlSecureTokenGenerator(),
                new Sha256TokenHasher(), new UserAuthVersionStore(redis, keys), JsonMapper.builder().findAndAddModules().build(),
                Clock.systemUTC());

        RefreshSessionException failure = catchThrowableOfType(RefreshSessionException.class,
                () -> store.requirePresented(sessionId + "." + "a".repeat(64)));

        assertThat(failure.getReason()).isEqualTo(RefreshSessionException.Reason.UNAVAILABLE);
    }
}
