package com.syncturtle.platform.tests.env;

import java.time.Duration;
import java.util.List;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

public final class SyncturtleContainers implements AutoCloseable {

    public final Network network = Network.newNetwork();

    @SuppressWarnings("resource")
    public final RedisContainer redis = new RedisContainer(
            DockerImageName.parse("redis").withTag("7-alpine"))
            .withNetwork(network)
            .withNetworkAliases("redis")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @SuppressWarnings("resource")
    public final GenericContainer<?> discoveryServer = new GenericContainer<>(
            DockerImageName.parse("syncturtle/discovery-server:local"))
            .withNetwork(network)
            .withNetworkAliases("discovery-server")
            .withExposedPorts(8761)
            .waitingFor(Wait.forHttp("/actuator/health")
                    .forPort(8761).forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @SuppressWarnings("resource")
    public final GenericContainer<?> configServer = new GenericContainer<>(
            DockerImageName.parse("syncturtle/config-server:local"))
            .withNetwork(network)
            .withNetworkAliases("config-server")
            .withExposedPorts(8888)
            .withEnv("SPRING_PROFILES_ACTIVE", "native")
            .waitingFor(Wait.forHttp("/actuator/health")
                    .forPort(8888).forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @SuppressWarnings("resource")
    public final GenericContainer<?> gateway = new GenericContainer<>(
            DockerImageName.parse("syncturtle/api-gateway:local"))
            .withNetwork(network)
            .withNetworkAliases("api-gateway")
            .withExposedPorts(8000)
            .withEnv("SPRING_PROFILES_ACTIVE", "docker")
            .withEnv("CONFIG_SERVER_URL", "http://config-server:8888")
            .withEnv("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", "http://discovery-server:8761/eureka")
            .withEnv("REDIS_HOST", "redis")
            .waitingFor(Wait.forHttp("/actuator/health")
                    .forPort(8000).forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    public List<Container<?>> all() {
        return List.of(redis, discoveryServer, configServer, gateway);
    }

    @Override
    public void close() throws Exception {
        try {
            gateway.close();
        } catch (Exception ignore) {
        }
        try {
            configServer.close();
        } catch (Exception ignore) {
        }
        try {
            discoveryServer.close();
        } catch (Exception ignore) {
        }
        try {
            redis.close();
        } catch (Exception ignore) {
        } finally {
            network.close();
        }
    }

    public String gatewayBaseUrl() {
        return "http://%s:%d".formatted(gateway.getHost(), gateway.getMappedPort(8000));
    }

}
