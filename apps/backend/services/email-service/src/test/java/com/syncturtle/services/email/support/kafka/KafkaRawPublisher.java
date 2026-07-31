package com.syncturtle.services.email.support.kafka;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Publishes exact JSON bytes so tests can exercise consumer deserialization.
 */
public final class KafkaRawPublisher implements AutoCloseable {

    private final KafkaProducer<String, byte[]> producer;

    private KafkaRawPublisher(KafkaProducer<String, byte[]> producer) {
        this.producer = producer;
    }

    public static KafkaRawPublisher connect(String bootstrapServers) {
        Objects.requireNonNull(bootstrapServers, "bootstrapServers is required");

        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        return new KafkaRawPublisher(new KafkaProducer<>(properties));
    }

    public void sendJson(String topic, String key, String json) {
        sendJson(topic, key, json, null);
    }

    public void sendJson(String topic, String key, String json, Headers headers) {
        Objects.requireNonNull(topic, "topic is required");
        Objects.requireNonNull(json, "json is required");

        Headers effectiveHeaders = headers == null ? new RecordHeaders() : headers;

        ProducerRecord<String, byte[]> record = new ProducerRecord<>(
                topic,
                null,
                key,
                json.getBytes(StandardCharsets.UTF_8),
                effectiveHeaders);

        try {
            producer.send(record).get(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to publish Kafka test record", exception);
        }
    }

    @Override
    public void close() {
        producer.close(Duration.ofSeconds(2));
    }
}
