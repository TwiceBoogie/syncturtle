package com.syncturtle.platform.tests.env;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

public final class ContainerLogs {

    private static final Logger log = LoggerFactory.getLogger(ContainerLogs.class);

    private ContainerLogs() {
        throw new AssertionError(
                "ContainerLogs must not be instantiated");
    }

    public static void dump(
            String title,
            List<? extends Container<?>> containers) {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(
                containers,
                "containers must not be null");

        log.error(
                "========== E2E FAILURE: {} ==========",
                title);

        for (Container<?> container : containers) {
            dumpContainer(container);
        }

        log.error(
                "========== END E2E FAILURE LOGS ==========");
    }

    private static void dumpContainer(Container<?> container) {
        try {
            log.error(
                    "----- CONTAINER: {} -----",
                    container.getContainerName());

            log.error("{}", container.getLogs());
        } catch (Exception exception) {
            log.error(
                    "Failed to dump logs for a test container",
                    exception);
        }
    }
}