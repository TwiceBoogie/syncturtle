package com.syncturtle.testing.containers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * One Postgres container for the entire JVM.
 * 
 * We still allow each service/test-suite to have its own logical DB inside
 * Postgres
 * to avoid cross module cross test contamination when running maven reactor
 * builds
 */
public final class PostgresContainerSingleton {

    // This db is just the default connection db; we create additional dbs
    // dynamically
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("postgres").withUsername("test").withPassword("test");

    private static final Set<String> CREATED_DB = ConcurrentHashMap.newKeySet();

    static {
        INSTANCE.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                INSTANCE.stop();
            } catch (Exception e) {
            }
        }));
    }

    private PostgresContainerSingleton() {
    }

    public static PostgreSQLContainer<?> getInstance() {
        return INSTANCE;
    }

    // Ensure DB exists; safe to call multiple times
    public static void ensureDatabase(String dbName) {
        if (!CREATED_DB.add(dbName))
            return;

        // connect to default "postgres" database and create our db
        String adminJdbc = INSTANCE.getJdbcUrl();
        try (Connection c = DriverManager.getConnection(adminJdbc, INSTANCE.getUsername(), INSTANCE.getPassword());
                Statement st = c.createStatement()) {
            st.execute("CREATE DATABASE \"" + dbName + "\"");
        } catch (Exception e) {
            // if parallel tests race, DB might already exist.
            // CREATED_DB prevents it from happening
            throw new RuntimeException("Failed creating test database: " + dbName, e);
        }
    }

    /** Build a JDBC URL that points to a specific DB inside the same container */
    public static String jdbcUrlForDb(String dbName) {
        String base = INSTANCE.getJdbcUrl();
        int slash = base.lastIndexOf('/');
        return base.substring(0, slash + 1) + dbName;
    }
}
