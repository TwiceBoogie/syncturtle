package com.syncturtle.testing.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public final class RedisContainerSingleton {

    private static final int REDIS_PORT = 6379;

    @SuppressWarnings("resource")
    private static final GenericContainer<?> INSTANCE = new GenericContainer<>(
            DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(REDIS_PORT);

    static {
        INSTANCE.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                INSTANCE.stop();
            } catch (Exception ignored) {
            }
        }));
    }

    private RedisContainerSingleton() {
    }

    public static GenericContainer<?> getInstance() {
        return INSTANCE;
    }

    public static String host() {
        return INSTANCE.getHost();
    }

    public static Integer port() {
        return INSTANCE.getMappedPort(REDIS_PORT);
    }

    public static String redisUri() {
        return "redis://%s:%d".formatted(host(), port());
    }

}
