package com.syncturtle.testing.annotations.impl;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.util.StringUtils;

import com.syncturtle.testing.annotations.UseKafka;
import com.syncturtle.testing.containers.KafkaContainerSingleton;

public class UseKafkaContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        UseKafka annotation = findAnnotation(testClass);
        if (annotation == null) {
            return null;
        }

        return (context, mergedConfig) -> {
            KafkaContainerSingleton.getInstance();

            String base = testClass.getSimpleName()
                    .replace('$', '-')
                    .toLowerCase(Locale.ROOT);

            String consumerGroup = StringUtils.hasText(annotation.consumerGroup())
                    ? annotation.consumerGroup()
                    : base + "-consumer";

            String configBroadcastGroup = StringUtils.hasText(annotation.configBroadcastGroup())
                    ? annotation.configBroadcastGroup()
                    : base + "-config";

            TestPropertyValues.of(
                    "spring.kafka.bootstrap-servers=" + KafkaContainerSingleton.bootstrapServers(),
                    "app.kafka.enabled=true",
                    "spring.kafka.listener.auto-startup=true",
                    "spring.kafka.consumer.auto-offset=earliest",
                    "spring.kafka.listener.concurrency=1",
                    "app.kafka.consumer-group=" + consumerGroup,
                    "app.kafka.config-broadcast-group=" + configBroadcastGroup).applyTo(context.getEnvironment());
        };
    }

    private UseKafka findAnnotation(Class<?> testClass) {
        UseKafka annotation = AnnotatedElementUtils.findMergedAnnotation(testClass, UseKafka.class);

        if (annotation == null && testClass.getEnclosingClass() != null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(testClass.getEnclosingClass(), UseKafka.class);
        }

        return annotation;
    }

}
