package com.syncturtle.testing.container;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One Postgres container for the entire JVM.
 * 
 * We still allow each service/test-suite to have its own logical DB inside
 * Postgres
 * to avoid cross module cross test contamination when running maven reactor
 * builds
 */
public final class PostgresContainerSingleton {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";
    private static final String DEFAULT_DATABASE = "postgres";
    private static final String TEST_USERNAME = "test";
    private static final String TEST_PASSWORD = "test";

    private static final Pattern SAFE_DATABASE_NAME = Pattern.compile("[a-zA-Z0-9_-]+");

    // This db is just the default connection db; we create additional dbs
    // dynamically
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer INSTANCE = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName(DEFAULT_DATABASE)
            .withUsername(TEST_USERNAME)
            .withPassword(TEST_PASSWORD);

    private static final Set<String> CREATED_DATABASES = ConcurrentHashMap.newKeySet();

    static {
        INSTANCE.start();
        Runtime.getRuntime().addShutdownHook(
                new Thread(PostgresContainerSingleton::stopContainer, "postgres-testcontainer-shutdown"));
    }

    private PostgresContainerSingleton() {
        throw new AssertionError("PostgresContainerSingletone must not be instantiated");
    }

    /**
     * Returns the shared, already started postgresql container
     * 
     * @return the shared postgresql container
     */
    public static PostgreSQLContainer getInstance() {
        return INSTANCE;
    }

    /**
     * Ensures that a logical database exists inside the shared container.
     * 
     * <p>
     * Synchronized because creating a database is a one-time operation. it prevents
     * parallel test contexts from trying to create the same database concurrently
     * 
     * @param databaseName
     */
    public static synchronized void ensureDatabase(String databaseName) {
        String normalizedDatabaseName = normalizeDatabaseName(databaseName);

        if (CREATED_DATABASES.contains(normalizedDatabaseName)) {
            return;
        }

        try (Connection connection = openAdministrativeConnection()) {
            if (!databaseExists(connection, normalizedDatabaseName)) {
                createDatabase(connection, normalizedDatabaseName);
            }

            CREATED_DATABASES.add(normalizedDatabaseName);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create test database: " + normalizedDatabaseName, exception);
        }
    }

    /**
     * Builds a JDBC URL pointing to a logical database inside the shared postgresql
     * container.
     * 
     * @param databaseName target logical database
     * @return JDBC URL for the target database
     */
    public static String jdbcUrlForDb(String databaseName) {
        String normalizedDatabaseName = normalizeDatabaseName(databaseName);

        String baseJdbcUrl = INSTANCE.getJdbcUrl();
        int queryIndex = baseJdbcUrl.indexOf('?');

        String queryParameters = queryIndex >= 0
                ? baseJdbcUrl.substring(queryIndex)
                : "";

        String jdbcUrlWithoutQuery = queryIndex >= 0
                ? baseJdbcUrl.substring(0, queryIndex)
                : baseJdbcUrl;

        int databasePathIndex = jdbcUrlWithoutQuery.lastIndexOf('/');

        if (databasePathIndex < 0) {
            throw new IllegalStateException("Unexpected PostgreSQL JDBC URL: " + baseJdbcUrl);
        }

        return jdbcUrlWithoutQuery.substring(0, databasePathIndex + 1) + normalizedDatabaseName + queryParameters;
    }

    private static Connection openAdministrativeConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(
                INSTANCE.getJdbcUrl(),
                INSTANCE.getUsername(),
                INSTANCE.getPassword());

        // CREATE DATBASE cannot execute inside a postgresql transaction.
        connection.setAutoCommit(true);

        return connection;
    }

    private static boolean databaseExists(Connection connection, String databaseName) throws SQLException {
        String sql = """
                SELECT 1
                FROM pg_database
                WHERE datname = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, databaseName);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void createDatabase(Connection connection, String databaseName) throws SQLException {
        String sql = "CREATE DATABASE \"" + databaseName + "\"";

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String normalizeDatabaseName(String rawDatabaseName) {
        String databaseName = Objects.requireNonNull(rawDatabaseName, "databaseName must not be null").trim();

        if (databaseName.isEmpty()) {
            throw new IllegalArgumentException("databaseName must not be blank");
        }
        if (!SAFE_DATABASE_NAME.matcher(databaseName).matches()) {
            throw new IllegalArgumentException("databaseName contains unsupported characters: " + databaseName);
        }

        return databaseName;
    }

    private static void stopContainer() {
        if (INSTANCE.isRunning()) {
            INSTANCE.stop();
        }
    }
}
