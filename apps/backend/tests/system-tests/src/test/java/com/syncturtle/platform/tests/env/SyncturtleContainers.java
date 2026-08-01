package com.syncturtle.platform.tests.env;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public final class SyncturtleContainers implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SyncturtleContainers.class);

    private static final int REDIS_PORT = 6379;
    private static final int DISCOVERY_SERVER_PORT = 8761;
    private static final int CONFIG_SERVER_PORT = 8888;
    private static final int API_GATEWAY_PORT = 8000;

    private static final Duration INFRASTRUCTURE_STARTUP_TIMEOUT = Duration.ofMinutes(2);

    public final Network network = Network.newNetwork();

    @SuppressWarnings("resource")
    public final GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withNetwork(network)
            .withNetworkAliases("redis")
            .withExposedPorts(REDIS_PORT)
            .waitingFor(Wait.forListeningPort());

    @SuppressWarnings("resource")
    public final GenericContainer<?> discoveryServer = new GenericContainer<>(
            DockerImageName.parse("syncturtle/discovery-server:local"))
            .withNetwork(network)
            .withNetworkAliases("discovery-server")
            .withExposedPorts(DISCOVERY_SERVER_PORT)
            .waitingFor(
                    Wait.forHttp("/actuator/health")
                            .forPort(DISCOVERY_SERVER_PORT)
                            .forStatusCode(200)
                            .withStartupTimeout(INFRASTRUCTURE_STARTUP_TIMEOUT));

    @SuppressWarnings("resource")
    public final GenericContainer<?> configServer = new GenericContainer<>(
            DockerImageName.parse("syncturtle/config-server:local"))
            .withNetwork(network)
            .withNetworkAliases("config-server")
            .withExposedPorts(CONFIG_SERVER_PORT)
            .withEnv("SPRING_PROFILES_ACTIVE", "native")
            .waitingFor(
                    Wait.forHttp("/actuator/health")
                            .forPort(CONFIG_SERVER_PORT)
                            .forStatusCode(200)
                            .withStartupTimeout(INFRASTRUCTURE_STARTUP_TIMEOUT));

    @SuppressWarnings("resource")
    public final GenericContainer<?> gateway = new GenericContainer<>(
            DockerImageName.parse("syncturtle/api-gateway:local"))
            .withNetwork(network)
            .withNetworkAliases("api-gateway")
            .withExposedPorts(API_GATEWAY_PORT)
            .withEnv("SPRING_PROFILES_ACTIVE", "docker")
            .withEnv("CONFIG_SERVER_URL", "http://config-server:8888")
            .withEnv("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", "http://discovery-server:8761/eureka")
            .withEnv("REDIS_HOST", "redis")
            .withEnv("REDIS_PORT", Integer.toString(REDIS_PORT))
            .waitingFor(Wait.forHttp("/actuator/health")
                    .forPort(API_GATEWAY_PORT)
                    .forStatusCode(200)
                    .withStartupTimeout(INFRASTRUCTURE_STARTUP_TIMEOUT));

    private boolean started;

    public synchronized void start() {
        if (started) {
            return;
        }

        try {
            redis.start();
            ;
            discoveryServer.start();
            configServer.start();
            gateway.start();

            started = true;
        } catch (RuntimeException exception) {
            ContainerLogs.dump("environment startup", all());
            close();

            throw new IllegalStateException("Failed to start the Syncturtle system-test environment", exception);
        }
    }

    public String gatewayBaseUrl() {
        if (!gateway.isRunning()) {
            throw new IllegalStateException("API gateway container is not running");
        }

        return "http://%s:%d".formatted(gateway.getHost(), gateway.getMappedPort(API_GATEWAY_PORT));
    }

    public List<Container<?>> all() {
        return List.<Container<?>>of(redis, discoveryServer, configServer, gateway);
    }

    @Override
    public synchronized void close() {
        // stop in reverse dependency order
        closeQuietly(gateway, "api-gateway");
        closeQuietly(configServer, "config-server");
        closeQuietly(discoveryServer, "discovery-server");
        closeQuietly(redis, "redis");
        closeQuietly(network, "network");

        started = false;
    }

    private static void closeQuietly(AutoCloseable resource, String resourceName) {
        try {
            resource.close();
        } catch (Exception exception) {
            log.warn("Failed to close system-test resources: {}", resourceName, exception);
        }
    }

}
