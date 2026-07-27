package com.syncturtle.common.kafka.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.RecordInterceptor;

import com.syncturtle.common.kafka.interceptor.MdcKafkaHeaderRecordInterceptor;

@AutoConfiguration(after = KafkaErrorHandlingAutoConfiguration.class)
@ConditionalOnClass({
        ConcurrentKafkaListenerContainerFactory.class,
        RecordInterceptor.class,
        MdcKafkaHeaderRecordInterceptor.class
})
@ConditionalOnBooleanProperty(prefix = "app.kafka", name = "enabled")
public class KafkaMdcConsumerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MdcKafkaHeaderRecordInterceptor.class)
    MdcKafkaHeaderRecordInterceptor<Object, Object> mdcKafkaHeaderRecordInterceptor() {

        return new MdcKafkaHeaderRecordInterceptor<>();
    }

    @Bean
    static BeanPostProcessor kafkaRecordInterceptorPostProcessor(
            MdcKafkaHeaderRecordInterceptor<Object, Object> mdcInterceptor) {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(
                    Object bean,
                    String beanName) throws BeansException {
                if (bean instanceof ConcurrentKafkaListenerContainerFactory<?, ?> factory) {

                    configureFactory(factory, mdcInterceptor);
                }

                return bean;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static void configureFactory(
            ConcurrentKafkaListenerContainerFactory<?, ?> factory,
            MdcKafkaHeaderRecordInterceptor<Object, Object> mdcInterceptor) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> typedFactory = (ConcurrentKafkaListenerContainerFactory<Object, Object>) factory;

        /*
         * Do not silently replace an interceptor explicitly configured
         * by an application.
         *
         * An application that needs both can configure a
         * CompositeRecordInterceptor itself.
         */
        if (typedFactory.getRecordInterceptor() == null) {
            typedFactory.setRecordInterceptor(mdcInterceptor);
        }
    }
}