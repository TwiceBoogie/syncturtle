package com.syncturtle.testing.annotation.impl;

import java.lang.annotation.Inherited;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.syncturtle.testing.annotation.UsePostgresDb;
import com.syncturtle.testing.container.PostgresContainerSingleton;

/**
 * Creates a Spring TestContext {@link ContextCustomizer} for tests annotated
 * with {@link UsePostgresDb}.
 * 
 * <p>
 * The annotation identifies a logical postgresql database that should be made
 * available to the test:
 * 
 * <pre>{@code
 * @JpaIntegrationTest
 * @UsePostgresDb("email_service_inbox_it")
 * class EmailInboxStoreIT {
 * }
 * }</pre>
 * 
 * <p>
 * The test-support module runs one shared postgresql container per test JVM.
 * Each service or test suite can request a separate logical database inside
 * that container. This provides:
 * 
 * <ul>
 * <li>Faster execution than starting one container for every test class</li>
 * <li>Database isolation between service modules</li>
 * <li>Real postgresql behavior for locks and native SQL</li>
 * <li>Stable spring application context reuse</li>
 * </ul>
 * 
 * @see ContextCustomizer
 * @see ContextCustomizerFactory
 * @see MergedContextConfiguration
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/context/ContextCustomizer.html">
 *      Spring ContextCustomizer contract</a>
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/caching.html">
 *      Spring TestContext context caching</a>
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/context-customizers.html">
 *      Spring context customizer registration</a>
 * @see <a href=
 *      "https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/">
 *      Testcontainers singleton container pattern</a>
 */
public final class UsePostgresDbContextCustomizerFactory implements ContextCustomizerFactory {

    /**
     * Creates a postgresql context customizer when the test requires a logical
     * database through {@link UsePostgresDb}.
     * 
     * @return postgresql context customizer, or {@code null} when the annotation is
     *         absent
     */
    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
            List<ContextConfigurationAttributes> configurationAttributes) {
        UsePostgresDb annotation = findAnnotation(testClass);
        if (annotation == null) {
            return null;
        }

        String databaseName = normalizeDatabaseName(annotation.value());

        return new PostgresContextCustomizer(databaseName);
    }

    /**
     * Searches the current test class and all enclosing classes for
     * {@link UsePostgresDb}.
     * 
     * <p>
     * JUnit nested classes are not subclasses of their enclosing class.
     * Java's {@link Inherited} mechanism alone does not make an
     * annotation on an outer test visible from a nested test class.
     * 
     * @param testClass class at which annotation lookup begins
     * @return merged {@link UsePostgresDb} annotation, or {@code null}
     */
    private static UsePostgresDb findAnnotation(Class<?> testClass) {
        Class<?> currentClass = testClass;

        while (currentClass != null) {
            UsePostgresDb annotation = AnnotatedElementUtils.findMergedAnnotation(currentClass, UsePostgresDb.class);
            if (annotation != null) {
                return annotation;
            }

            currentClass = currentClass.getEnclosingClass();
        }

        return null;
    }

    private static String normalizeDatabaseName(String rawDatabaseName) {
        Assert.hasText(rawDatabaseName, "@UsePostgresDb database name must not be blank");

        return rawDatabaseName.trim();
    }

    /**
     * Value object representing the postgresql env requested by a test.
     * 
     * <p>
     * Only {@code databaseName} is stored because it is the annotation value
     * that changes the logical spring config. Container connection
     * values are resolved when the context is customized.
     */
    private static final class PostgresContextCustomizer implements ContextCustomizer {

        private final String databaseName;

        private PostgresContextCustomizer(String databaseName) {
            this.databaseName = databaseName;
        }

        /**
         * Ensures that the requested logical db exists and injects the
         * resulting datasource and liquibase props into spring's env.
         */
        @Override
        public void customizeContext(ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfiguration) {
            PostgreSQLContainer container = PostgresContainerSingleton.getInstance();
            PostgresContainerSingleton.ensureDatabase(databaseName);

            String jdbcUrl = PostgresContainerSingleton.jdbcUrlForDb(databaseName);
            String username = container.getUsername();
            String password = container.getPassword();

            TestPropertyValues.of(
                    "spring.datasource.url=" + jdbcUrl,
                    "spring.datasource.username=" + username,
                    "spring.datasource.password=" + password,
                    "spring.liquibase.url=" + jdbcUrl,
                    "spring.liquibase.user=" + username,
                    "spring.liquibase.password=" + password,
                    "spring.liquibase.enabled=true",
                    "spring.cloud.config.enabled=false",
                    "spring.cloud.config.import-check.enabled=false")
                    .applyTo(context.getEnvironment());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof PostgresContextCustomizer that)) {
                return false;
            }

            return databaseName.equals(that.databaseName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(databaseName);
        }

    }

}
