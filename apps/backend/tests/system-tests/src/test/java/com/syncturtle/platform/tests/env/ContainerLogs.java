package com.syncturtle.platform.tests.env;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

public final class ContainerLogs {

    private static final Logger log = LoggerFactory.getLogger(ContainerLogs.class);

    private ContainerLogs() {
    }

    public static void dump(String title, List<? extends Container<?>> containers) {
        log.error("========== E2E FAILURE: dumping container logs: {} ==========", title);
        for (Container<?> c : containers) {
            try {
                log.error("----- LOGS: {} -----", c.getContainerName());
                log.error(c.getLogs());
            } catch (Exception exception) {
                log.error("Failed to dump logs for container {}", c.getContainerName(), exception);
            }
        }
        log.error("========== END LOG DUMP ==========");
    }

}
