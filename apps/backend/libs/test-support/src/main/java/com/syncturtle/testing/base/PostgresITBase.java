package com.syncturtle.testing.base;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.syncturtle.testing.containers.PostgresContainerSingleton;

/**
 * Base for sppring integration tests that need Postgres.
 * 
 * Each service should override databaseName() so they don't share the same DB
 */
public abstract class PostgresITBase {

    protected static String databaseName() {
        return "default_it";
    }

    @BeforeAll
    static void startContainer() {
        PostgresContainerSingleton.getInstance();
        PostgresContainerSingleton.ensureDatabase(databaseName());
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        String db = databaseName();
        registry.add("spring.datasource.url", () -> PostgresContainerSingleton.jdbcUrlForDb(db));
        registry.add("spring.datasource.username", () -> PostgresContainerSingleton.getInstance().getUsername());
        registry.add("spring.datasource.password", () -> PostgresContainerSingleton.getInstance().getPassword());

        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
    }

}
