package com.syncturtle.platform.tests.env;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

public final class SyncturtleEnvironment implements BeforeAllCallback, AfterEachCallback {

    private static final Namespace NAMESPACE = Namespace.create(SyncturtleEnvironment.class);

    // wrap containers so junit auto calls close() at the end of the test
    private static final class ManagedEnv implements CloseableResource {

        final SyncturtleContainers containers = new SyncturtleContainers();
        boolean started = false;

        void startOnce() {
            if (started) {
                return;
            }
            containers.redis.start();
            containers.discoveryServer.start();
            containers.configServer.start();
            containers.gateway.start();
            started = true;
        }

        @Override
        public void close() throws Throwable {
            containers.close();
        }

    }

    private static ManagedEnv env(ExtensionContext context) {
        Store store = context.getRoot().getStore(NAMESPACE);
        return store.getOrComputeIfAbsent(ManagedEnv.class, key -> new ManagedEnv(), ManagedEnv.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        env(context).startOnce();
        // export base urls so tests can read them without ExtensionContext
        System.setProperty("syncturtle.gateway.base-url", env(context).containers.gatewayBaseUrl());
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        context.getExecutionException()
                .ifPresent(exception -> ContainerLogs.dump(context.getDisplayName(), env(context).containers.all()));
    }

    public static String gatewayBaseUrl(ExtensionContext context) {
        return env(context).containers.gatewayBaseUrl();
    }

}
