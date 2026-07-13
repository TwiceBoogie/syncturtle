package com.syncturtle.common.kafka.interceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

import com.syncturtle.common.core.header.GatewayHeaders;

public final class MdcKafkaHeaderProducerInterceptor
        implements ProducerInterceptor<Object, Object> {

    public static final String MDC_CORRELATION_ID = GatewayHeaders.HDR_CORRELATION_ID;

    public static final String MDC_REQUEST_ID = GatewayHeaders.HDR_REQUEST_ID;

    @Override
    public ProducerRecord<Object, Object> onSend(
            ProducerRecord<Object, Object> record) {
        if (record == null) {
            return null;
        }

        addMdcHeader(
                record,
                MDC_CORRELATION_ID,
                GatewayHeaders.HDR_CORRELATION_ID);

        addMdcHeader(
                record,
                MDC_REQUEST_ID,
                GatewayHeaders.HDR_REQUEST_ID);

        return record;
    }

    @Override
    public void onAcknowledgement(
            RecordMetadata metadata,
            Exception exception) {
        // No acknowledgement handling required.
    }

    @Override
    public void configure(Map<String, ?> configurations) {
        // No external configuration required.
    }

    @Override
    public void close() {
        // No resources to close.
    }

    private static void addMdcHeader(
            ProducerRecord<Object, Object> record,
            String mdcKey,
            String headerName) {
        String value = getMdcValue(mdcKey);

        if (value == null) {
            return;
        }

        record.headers().remove(headerName);

        record.headers().add(
                headerName,
                value.getBytes(StandardCharsets.UTF_8));
    }

    private static String getMdcValue(String key) {
        String value = MDC.get(key);

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}