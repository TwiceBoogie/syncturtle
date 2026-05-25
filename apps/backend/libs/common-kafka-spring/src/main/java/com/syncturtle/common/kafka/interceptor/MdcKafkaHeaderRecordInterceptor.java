package com.syncturtle.common.kafka.interceptor;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;

import com.syncturtle.common.core.header.GatewayHeaders;

public final class MdcKafkaHeaderRecordInterceptor<K, V> implements RecordInterceptor<K, V> {

    @Override
    public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
        putHeaderInMdc(record, GatewayHeaders.HDR_CORRELATION_ID);
        putHeaderInMdc(record, GatewayHeaders.HDR_REQUEST_ID);
        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
        MDC.remove(GatewayHeaders.HDR_CORRELATION_ID);
        MDC.remove(GatewayHeaders.HDR_REQUEST_ID);
    }

    private static void putHeaderInMdc(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null || header.value() == null) {
            return;
        }

        String value = new String(header.value(), StandardCharsets.UTF_8);
        if (!value.isEmpty()) {
            MDC.put(headerName, value);
        }
    }

}
