package com.syncturtle.testing.annotation.impl;

import java.util.List;
import java.util.Locale;

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
 * Starts shared Kafka infrastructure with an isolated installation identity per
 * test context.
 */
public final class UseKafkaContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        Class<?> current = testClass;
        while (current != null) {
            UseKafka annotation = AnnotatedElementUtils.findMergedAnnotation(current, UseKafka.class);
            if (annotation != null) {
                String deploymentId = StringUtils.hasText(annotation.deploymentId())
                        ? annotation.deploymentId().trim()
                        : current.getName().replace('$', '-').replace('.', '-').toLowerCase(Locale.ROOT);
                return new KafkaContextCustomizer(deploymentId);
            }
            current = current.getEnclosingClass();
        }
        return null;
    }

    private static final class KafkaContextCustomizer implements ContextCustomizer {
        private final String deploymentId;

        private KafkaContextCustomizer(String deploymentId) {
            this.deploymentId = deploymentId;
        }

        @Override
        public void customizeContext(ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfiguration) {
            KafkaContainerSingleton.getInstance();
            TestPropertyValues.of(
                    "spring.kafka.bootstrap-servers=" + KafkaContainerSingleton.bootstrapServers(),
                    "spring.kafka.consumer.auto-offset-reset=earliest",
                    "spring.kafka.listener.auto-startup=true",
                    "spring.kafka.listener.concurrency=1",
                    "app.kafka.deployment-id=" + deploymentId,
                    "app.kafka.instance-configuration.deployment-id=" + deploymentId)
                    .applyTo(context.getEnvironment());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || (other instanceof KafkaContextCustomizer that
                    && deploymentId.equals(that.deploymentId));
        }

        @Override
        public int hashCode() {
            return deploymentId.hashCode();
        }
    }

}
