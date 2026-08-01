package com.syncturtle.testing.annotation.impl;

import java.lang.annotation.Inherited;
import java.util.List;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

import com.syncturtle.testing.annotation.UseRedis;
import com.syncturtle.testing.container.RedisContainerSingleton;

/**
 * Creates a Spring TestContext {@link ContextCustomizer} for tests annotated
 * with {@link UseRedis}.
 *
 * <p>
 * A test opts into real Redis infrastructure by declaring:
 *
 * <pre>{@code
 * @IntegrationTest
 * @UseRedis
 * class EmailRuntimeConfigCacheIT {
 * }
 * }</pre>
 *
 * <p>
 * The resulting customizer starts or obtains the shared Redis Testcontainer
 * and supplies its mapped connection properties to Spring Boot before the
 * application context is refreshed.
 *
 * <h2>Why the customizer is stateless</h2>
 *
 * <p>
 * Unlike {@link UseKafka} and {@code @UsePostgresDb}, {@link UseRedis} contains
 * no configuration attributes. Every test annotated with {@code @UseRedis}
 * therefore requests the same logical Redis environment.
 *
 * <p>
 * This factory returns one stateless customizer instance. Its equality rule
 * considers every {@link RedisContextCustomizer} equivalent. This allows
 * otherwise identical Redis integration tests to reuse Spring's cached
 * application context.
 *
 * <h2>Why container host and port are not equality fields</h2>
 *
 * <p>
 * The mapped host and port are runtime details of
 * {@link RedisContainerSingleton}; they are not choices made by the test
 * class. They are resolved only when the customizer applies properties to the
 * context.
 *
 * <p>
 * If {@link UseRedis} later gains configurable attributes, such as a database
 * index or authentication mode, those attributes must be added as fields on
 * the customizer and included in both {@link #equals(Object)} and
 * {@link #hashCode()}.
 * 
 * @see ContextCustomizer
 * @see ContextCustomizerFactory
 * @see MergedContextConfiguration
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/context/ContextCustomizer.html">
 *      Spring ContextCustomizer contract</a>
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/caching.html">
 *      Spring TestContext context caching</a>
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/context-customizers.html">
 *      Spring context customizer registration</a>
 * @see <a href=
 *      "https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/">
 *      Testcontainers singleton container pattern</a>
 */
public final class UseRedisContextCustomizerFactory implements ContextCustomizerFactory {

    private static final ContextCustomizer REDIS_CONTEXT_CUSTOMIZER = new RedisContextCustomizer();

    /**
     * Creates the redis context customization when {@link UseRedis} exists on
     * the test class or one of its enclosing test classes.
     * 
     * @return shared redis customizer, or {@code null} when redis was not requested
     */
    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        UseRedis annotation = findAnnotation(testClass);
        if (annotation == null) {
            return null;
        }

        return REDIS_CONTEXT_CUSTOMIZER;
    }

    /**
     * Searches the current test class and all enclosing classes for
     * {@link UseRedis}.
     * 
     * <p>
     * JUnit nested classes are not subclasses of their enclosing class.
     * Java's {@link Inherited} mechanism alone does not make an
     * annotation on an outer test visible from a nested test class.
     * 
     * @param testClass class at which annotation lookup begins
     * @return merged {@link UseRedis} annotation, or {@code null}
     */
    private UseRedis findAnnotation(Class<?> testClass) {
        Class<?> currentClass = testClass;

        while (currentClass != null) {
            UseRedis annotation = AnnotatedElementUtils.findMergedAnnotation(currentClass, UseRedis.class);
            if (annotation != null) {
                return annotation;
            }

            currentClass = currentClass.getEnclosingClass();
        }

        return null;
    }

    /**
     * Stateless customizer that contributes the singleton Redis container's
     * runtime connection values to a spring test context.
     */
    private static final class RedisContextCustomizer implements ContextCustomizer {

        /**
         * Retrieves the shared Redis container and applies its mapped
         * connection info before spring boot creates redis clients,
         * connection factories, caches, and repositories.
         */
        @Override
        public void customizeContext(ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfiguration) {
            // trigger singleton initialization before reading the mapped host and port
            RedisContainerSingleton.getInstance();

            TestPropertyValues.of(
                    "spring.data.redis.host=" + RedisContainerSingleton.host(),
                    "spring.data.redis.port=" + RedisContainerSingleton.port(),
                    "spring.data.redis.url=" + RedisContainerSingleton.redisUri())
                    .applyTo(context.getEnvironment());
        }

        /**
         * Treats every redis customizer as equivalent because {@link UseRedis}
         * currently has no configurable attributes.
         */
        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof RedisContextCustomizer;
        }

        @Override
        public int hashCode() {
            return RedisContextCustomizer.class.hashCode();
        }

    }

}
