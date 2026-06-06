package com.syncturtle.common.spring.autoconfigure.observability.kafka;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.RecordInterceptor;

@AutoConfiguration
@ConditionalOnClass({
        ConcurrentKafkaListenerContainerFactory.class,
        RecordInterceptor.class,
        MdcKafkaHeaderRecordInterceptor.class
})
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaMdcConsumerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MdcKafkaHeaderRecordInterceptor.class)
    MdcKafkaHeaderRecordInterceptor<Object, Object> mdcKafkaHeaderRecordInterceptor() {
        return new MdcKafkaHeaderRecordInterceptor<>();
    }

    @Bean
    static BeanPostProcessor kafkaRecordInterceptorPostProcessor(
            MdcKafkaHeaderRecordInterceptor<Object, Object> mdcKafkaHeaderRecordInterceptor) {
        return new BeanPostProcessor() {
            @Override
            @SuppressWarnings("unchecked")
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof ConcurrentKafkaListenerContainerFactory<?, ?> factory) {
                    ((ConcurrentKafkaListenerContainerFactory<Object, Object>) factory)
                            .setRecordInterceptor(mdcKafkaHeaderRecordInterceptor);
                }

                return bean;
            }
        };
    }

}
