package com.syncturtle.testing.containers;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public final class KafkaContainerSingleton {

    private static final KafkaContainer INSTANCE = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:4.0.0"));

    static {
        INSTANCE.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                INSTANCE.stop();
            } catch (Exception ignored) {
            }
        }));
    }

    private KafkaContainerSingleton() {
    }

    public static KafkaContainer getInstance() {
        return INSTANCE;
    }

    public static String bootstrapServers() {
        return INSTANCE.getBootstrapServers();
    }

}
