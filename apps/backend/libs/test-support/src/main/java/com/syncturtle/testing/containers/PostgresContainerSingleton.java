package com.syncturtle.testing.containers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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

    private static final Pattern SAFE_DB_NAME = Pattern.compile("[a-zA-Z0-9_\\-]+");

    // This db is just the default connection db; we create additional dbs
    // dynamically
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("postgres")
            .withUsername("test")
            .withPassword("test");

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
        String normalized = normalizeDbName(dbName);

        if (!CREATED_DB.add(normalized)) {
            return;
        }

        // connect to default "postgres" database and create the db
        String adminJdbc = INSTANCE.getJdbcUrl();
        try (Connection connection = DriverManager.getConnection(
                adminJdbc,
                INSTANCE.getUsername(),
                INSTANCE.getPassword())) {
            if (databaseExists(connection, normalized)) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE \"" + normalized + "\"");
            }
        } catch (Exception e) {
            // if parallel tests race, DB might already exist.
            // CREATED_DB prevents it from happening
            throw new IllegalStateException("Failed creating test database: " + dbName, e);
        }
    }

    /** Build a JDBC URL that points to a specific DB inside the same container */
    public static String jdbcUrlForDb(String dbName) {
        String normalized = normalizeDbName(dbName);

        String base = INSTANCE.getJdbcUrl();
        int queryIndex = base.indexOf('?');
        String query = queryIndex >= 0 ? base.substring(queryIndex) : "";
        String withoutQuery = queryIndex >= 0 ? base.substring(0, queryIndex) : base;

        int slash = withoutQuery.lastIndexOf('/');
        if (slash < 0) {
            throw new IllegalStateException("Unexpected PostgreSQL JDBC URL: " + base);
        }

        return withoutQuery.substring(0, slash + 1) + normalized + query;
    }

    private static boolean databaseExists(Connection connection, String dbName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("select 1 from pg_database where datname = ?")) {
            statement.setString(1, dbName);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static String normalizeDbName(String raw) {
        String dbName = Objects.requireNonNull(raw, "dbName must not be null").trim();

        if (dbName.isEmpty()) {
            throw new IllegalArgumentException("dbName must not be blank");
        }
        if (!SAFE_DB_NAME.matcher(dbName).matches()) {
            throw new IllegalArgumentException("dbName contains unsupported characters: " + dbName);
        }

        return dbName;
    }
}
