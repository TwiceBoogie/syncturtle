package com.syncturtle.platform.tests.env;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

public final class SyncturtleEnvironment implements BeforeAllCallback, AfterEachCallback {

    private static final Namespace NAMESPACE = Namespace.create(SyncturtleEnvironment.class);
    private static final String GATEWAY_BASE_URL_PROPERTY = ServiceUrls.GATEWAY_BASE_URL;

    // wrap containers so junit auto calls close() at the end of the test
    private static final class ManagedEnvironment implements AutoCloseable {

        private final SyncturtleContainers containers = new SyncturtleContainers();
        private boolean started;

        synchronized void startOnce() {
            if (started) {
                return;
            }

            containers.start();
            started = true;
        }

        String gatewayBaseUrl() {
            if (!started) {
                throw new IllegalStateException("System-test environment has not been started");
            }

            return containers.gatewayBaseUrl();
        }

        void dumpLogs(String title) {
            ContainerLogs.dump(title, containers.all());
        }

        @Override
        public synchronized void close() {
            System.clearProperty(GATEWAY_BASE_URL_PROPERTY);

            containers.close();
            started = false;
        }

    }

    private static ManagedEnvironment environment(ExtensionContext context) {
        Store store = context.getRoot().getStore(NAMESPACE);
        return store.computeIfAbsent(ManagedEnvironment.class, ignored -> new ManagedEnvironment(),
                ManagedEnvironment.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        ManagedEnvironment environment = environment(context);

        environment.startOnce();
        // export base urls so tests can read them without ExtensionContext
        System.setProperty(GATEWAY_BASE_URL_PROPERTY, environment.gatewayBaseUrl());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        context.getExecutionException()
                .ifPresent(exception -> environment(context).dumpLogs(context.getDisplayName()));
    }

    public static String gatewayBaseUrl(ExtensionContext context) {
        return environment(context).containers.gatewayBaseUrl();
    }

}
