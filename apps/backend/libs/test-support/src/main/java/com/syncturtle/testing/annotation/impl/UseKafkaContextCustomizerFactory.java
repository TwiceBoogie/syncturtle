package com.syncturtle.testing.annotation.impl;

import java.lang.annotation.Inherited;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.StringUtils;

import com.syncturtle.testing.annotation.UseKafka;
import com.syncturtle.testing.container.KafkaContainerSingleton;

/**
 * Creates a Spring TestContext {@link ContextCustomizer} for tests annotated
 * with {@link UseKafka}.
 * 
 * <p>
 * A {@code ContextCustomizerFactory} runs while spring is preparing the
 * application context for a test class. It allows the shared test-support
 * module to contribute environment properties before Spring Boot creates
 * infrastructure beans such as:
 *
 * <ul>
 * <li>Kafka producer factories</li>
 * <li>Kafka consumer factories</li>
 * <li>Kafka listener containers</li>
 * <li>Kafka templates</li>
 * </ul>
 * 
 * <p>
 * This is preferable to requiring every integration test to repeat a
 * {@code @DynamicPropertySource} method. A test only needs to declare:
 *
 * <pre>{@code
 * @IntegrationTest
 * @UseKafka
 * class EmailToSendEventListenerIT {
 * }
 * }</pre>
 *
 * <p>
 * The factory performs two distinct responsibilities:
 *
 * <ol>
 * <li>
 * It reads and normalizes the declarative configuration from
 * {@link UseKafka}.
 * </li>
 * <li>
 * It creates a stable {@link ContextCustomizer} containing that
 * configuration.
 * </li>
 * </ol>
 * 
 * <h2>Why the customizer is a concrete class</h2>
 *
 * <p>
 * Spring includes every {@link ContextCustomizer} in the
 * {@link MergedContextConfiguration} used as the application-context cache
 * key. Therefore, implementations must have value-based {@link #equals(Object)}
 * and {@link #hashCode()} behavior.
 *
 * <p>
 * Returning a new lambda from this factory would give each lambda
 * identity-based equality. Two tests requesting the same Kafka configuration
 * could then appear different to Spring, preventing reuse of an otherwise
 * identical cached application context.
 * 
 * <p>
 * The concrete {@link KafkaContextCustomizer} solves that problem by defining
 * equality from the two values that materially affect the test context:
 *
 * <ul>
 * <li>The primary email-event consumer group</li>
 * <li>The configuration-broadcast consumer group</li>
 * </ul>
 *
 * <h2>Nested test support</h2>
 *
 * <p>
 * {@link Inherited} does not make annotations flow from an enclosing test
 * class to a nested class. Consequently, {@link #findAnnotation(Class)} walks
 * through enclosing classes explicitly. This allows an annotation placed on
 * an outer integration-test class to configure its JUnit {@code @Nested}
 * classes as well.
 *
 * <h2>Container lifecycle</h2>
 *
 * <p>
 * {@link KafkaContainerSingleton} owns the Kafka container lifecycle. Calling
 * {@link KafkaContainerSingleton#getInstance()} ensures that the singleton is
 * initialized and running before its mapped bootstrap address is added to the
 * Spring environment.
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
public class UseKafkaContextCustomizerFactory implements ContextCustomizerFactory {

    /**
     * Examines the test class and creates a Kafka context customizer when
     * {@link UseKafka} is present.
     *
     * <p>
     * Returning {@code null} is part of the
     * {@link ContextCustomizerFactory} contract. It tells spring that this
     * factory has no customization to contribute for the current test class.
     *
     * @param testClass               the test class whose context Spring is
     *                                preparing
     * @param configurationAttributes the collected context-configuration
     *                                attributes; not needed by this factory
     * @return a stable Kafka context customizer, or {@code null} when Kafka was not
     *         requested
     */
    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        UseKafka annotation = findAnnotation(testClass);
        if (annotation == null) {
            return null;
        }

        String base = testClass.getName()
                .replace('$', '-')
                .replace('.', '-')
                .toLowerCase(Locale.ROOT);
        String consumerGroup = resolveGroup(annotation.consumerGroup(), base + "-consumer");
        String configBroadcastGroup = resolveGroup(annotation.configBroadcastGroup(), base + "-config");

        return new KafkaContextCustomizer(consumerGroup, configBroadcastGroup);
    }

    /**
     * Searches the current test class and all enclosing classes for
     * {@link UseKafka}.
     * 
     * <p>
     * JUnit nested classes are not subclasses of their enclosing class.
     * Java's {@link Inherited} mechanism alone does not make an
     * annotation on an outer test visible from a nested test class.
     * 
     * @param testClass class at which annotation lookup begins
     * @return merged {@link UseKafka} annotation, or {@code null}
     */
    private static UseKafka findAnnotation(Class<?> testClass) {
        Class<?> current = testClass;

        while (current != null) {
            UseKafka annotation = AnnotatedElementUtils.findMergedAnnotation(current, UseKafka.class);
            if (annotation != null) {
                return annotation;
            }

            current = current.getEnclosingClass();
        }

        return null;
    }

    /**
     * Selects an explicitly configured group when it contains text, otherwise,
     * it returns the generated test specific group.
     * 
     * @param explicitGroup  group supplied through {@link UseKafka}
     * @param generatedGroup fallback generated from the test class
     * @return normalized Kafka consumer group
     */
    private static String resolveGroup(String explicitGroup, String generatedGroup) {
        return StringUtils.hasText(explicitGroup) ? explicitGroup.trim() : generatedGroup;
    }

    /**
     * Value object that applies Kafka specific test properties before the
     * spring application context is refreshed.
     * 
     * <p>
     * Equality is based on every field that can produce a different
     * spring context. Tests using the same group are therefore allowed to
     * share a cached context, test using different groups receive distinct
     * cache keys.
     */
    private static final class KafkaContextCustomizer implements ContextCustomizer {

        private final String consumerGroup;
        private final String configBroadcastGroup;

        private KafkaContextCustomizer(String consumerGroup, String configBroadcastGroup) {
            this.consumerGroup = consumerGroup;
            this.configBroadcastGroup = configBroadcastGroup;
        }

        /**
         * Starts or retrieves the shared Kafka container and contributes its
         * runtime connection information to the spring env.
         */
        @Override
        public void customizeContext(ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfiguration) {
            KafkaContainerSingleton.getInstance();

            TestPropertyValues.of(
                    "spring.kafka.bootstrap-servers=" + KafkaContainerSingleton.bootstrapServers(),
                    "spring.kafka.consumer.auto-offset-reset=earliest",
                    "spring.kafka.listener.auto-startup=true",
                    "spring.kafka.listener.concurrency=1",
                    "app.kafka.enabled=true",
                    "app.kafka.consumer-group=" + consumerGroup,
                    "app.kafka.config-broadcast-group=" + configBroadcastGroup)
                    .applyTo(context.getEnvironment());
        }

        /**
         * Determines whether two customizers represent the same Kafka test
         * environment.
         * 
         * @param other canditate customizer
         * @return {@code true} when both consumer groups are equal
         */
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof KafkaContextCustomizer that)) {
                return false;
            }

            return consumerGroup.equals(that.consumerGroup)
                    && configBroadcastGroup.equals(that.configBroadcastGroup);
        }

        @Override
        public int hashCode() {
            return Objects.hash(consumerGroup, configBroadcastGroup);
        }

    }

}
