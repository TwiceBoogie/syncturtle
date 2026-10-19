package com.syncturtle.services.instance.service.collaborator.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.syncturtle.common.cache.property.RedisKeyProperties;
import com.syncturtle.common.cache.template.RedisKeyBuilder;

class InstanceAdminSessionVersionWriterTest {

    private static final UUID INSTANCE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void staleWriteCannotLowerARevocationVersion() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        RedisKeyBuilder keys = new RedisKeyBuilder(new RedisKeyProperties("st:test:"));
        String key = keys.authKey("user-service", "admin-session-version", "i", INSTANCE.toString(),
                "u", USER.toString());
        when(values.get(key)).thenReturn("9");
        InstanceAdminSessionVersionWriter writer = new InstanceAdminSessionVersionWriter(redis, keys);

        IllegalStateException failure = catchThrowableOfType(IllegalStateException.class,
                () -> writer.write(INSTANCE, USER, 8L));

        assertThat(failure).hasMessageContaining("cannot regress");
        verify(values).get(key);
    }

}
