package com.syncturtle.common.spring.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

public class MdcKafkaHeaderProducerInterceptor implements ProducerInterceptor<String, Object> {

    // Header names (on the kafka record)
    public static final String HDR_CORRELATION_ID = "X-Correlation-Id";
    public static final String HDR_REQUEST_ID = "X-Request-Id";

    /**
     * MDC keys.
     * If you configure Micrometer "baggage correlation fields" using header names,
     * the MDC keys typically match the header names
     */
    public static final String MDC_CORRELATION_ID = "X-Correlation-Id";
    public static final String MDC_REQUEST_ID = "X-Request-Id";

    @Override
    public void configure(Map<String, ?> arg0) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public void onAcknowledgement(RecordMetadata arg0, Exception arg1) {
        // no-op
    }

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        if (record == null) {
            return null;
        }

        // correlation ID (end-to-end)
        String correlationId = safeMdcGet(MDC_CORRELATION_ID);
        if (correlationId != null) {
            upsertHeader(record, HDR_CORRELATION_ID, correlationId);
        }

        // request id (per-hop/per-request)
        String requestId = safeMdcGet(MDC_REQUEST_ID);
        if (requestId != null) {
            upsertHeader(record, HDR_REQUEST_ID, requestId);
        }

        return record;
    }

    private static String safeMdcGet(String key) {
        String value = MDC.get(key);
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    private static void upsertHeader(ProducerRecord<String, Object> record, String headerName, String value) {
        // remove existing to avoid duplicates if re-used/retried in some producer flows
        record.headers().remove(headerName);
        record.headers().add(headerName, value.getBytes(StandardCharsets.UTF_8));
    }

}
