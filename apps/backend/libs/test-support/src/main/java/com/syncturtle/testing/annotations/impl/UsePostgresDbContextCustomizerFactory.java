package com.syncturtle.testing.annotations.impl;

import java.util.List;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

import com.syncturtle.testing.annotations.UsePostgresDb;
import com.syncturtle.testing.containers.PostgresContainerSingleton;

public final class UsePostgresDbContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
            List<ContextConfigurationAttributes> configurationAttributes) {
        UsePostgresDb ann = AnnotatedElementUtils.findMergedAnnotation(testClass, UsePostgresDb.class);
        if (ann == null && testClass.getEnclosingClass() != null) {
            ann = AnnotatedElementUtils.findMergedAnnotation(testClass.getEnclosingClass(), UsePostgresDb.class);
        }

        if (ann == null) {
            return null;
        }
        String dbName = ann.value();
        return (context, mergedConfig) -> {
            PostgresContainerSingleton.getInstance();
            PostgresContainerSingleton.ensureDatabase(dbName);

            TestPropertyValues.of(
                    "spring.datasource.url=" + PostgresContainerSingleton.jdbcUrlForDb(dbName),
                    "spring.datasource.username=" + PostgresContainerSingleton.getInstance().getUsername(),
                    "spring.datasource.password=" + PostgresContainerSingleton.getInstance().getPassword(),
                    "spring.liquibase.enabled=true",
                    "spring.cloud.config.enabled=false",
                    "eureka.client.enabled=false").applyTo(context.getEnvironment());
        };
    }

}
