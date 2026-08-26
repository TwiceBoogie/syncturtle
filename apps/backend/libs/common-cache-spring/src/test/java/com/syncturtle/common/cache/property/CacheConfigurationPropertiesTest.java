package com.syncturtle.common.cache.property;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.cache.autoconfigure.RedisKeyAutoConfiguration;
import com.syncturtle.common.cache.template.RedisKeyBuilder;

class CacheConfigurationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsJavaDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(RedisKeyProperties.class).getKeyPrefix()).isEqualTo("st:local:");
            assertThat(context.getBean(ResponseCacheProperties.class).getHashBytes()).isEqualTo(16);
        });
    }

    @Test
    void bindsYamlStyleOverrides() {
        contextRunner
                .withPropertyValues(
                        "app.redis.keys.key-prefix=st:test",
                        "app.response-cache.hash-bytes=24")
                .run(context -> {
                    assertThat(context.getBean(RedisKeyProperties.class).getKeyPrefix()).isEqualTo("st:test:");
                    assertThat(context.getBean(ResponseCacheProperties.class).getHashBytes()).isEqualTo(24);
                });
    }

    @Test
    void autoConfigurationRegistersPropertiesAndConsumers() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RedisKeyAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RedisKeyProperties.class);
                    assertThat(context).hasSingleBean(RedisKeyBuilder.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ RedisKeyProperties.class, ResponseCacheProperties.class })
    static class TestConfiguration {

    }

}
