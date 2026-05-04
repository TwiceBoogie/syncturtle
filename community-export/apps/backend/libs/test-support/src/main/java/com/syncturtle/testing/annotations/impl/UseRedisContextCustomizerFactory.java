package com.syncturtle.testing.annotations.impl;

import java.util.List;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

import com.syncturtle.testing.annotations.UseRedis;
import com.syncturtle.testing.containers.RedisContainerSingleton;

public final class UseRedisContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        UseRedis annotation = findAnnotation(testClass);
        if (annotation == null) {
            return null;
        }

        return (context, mergedConfig) -> {
            RedisContainerSingleton.getInstance();

            TestPropertyValues.of(
                    "spring.data.redis.host=" + RedisContainerSingleton.host(),
                    "spring.data.redis.port=" + RedisContainerSingleton.port(),
                    "spring.data.redis.url=" + RedisContainerSingleton.redisUri()).applyTo(context.getEnvironment());
        };
    }

    private UseRedis findAnnotation(Class<?> testClass) {
        UseRedis annotation = AnnotatedElementUtils.findMergedAnnotation(testClass, UseRedis.class);

        if (annotation == null && testClass.getEnclosingClass() != null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(testClass.getEnclosingClass(), UseRedis.class);
        }

        return annotation;
    }

}
