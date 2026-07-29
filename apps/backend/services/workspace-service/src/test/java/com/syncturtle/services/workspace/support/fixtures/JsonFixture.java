package com.syncturtle.services.workspace.support.fixtures;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class JsonFixture {

    private JsonFixture() {
    }

    public static String read(String classpathLocation) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream is = cl.getResourceAsStream(classpathLocation)) {
            if (is == null) {
                throw new IllegalArgumentException("Fixture not found on classpath: " + classpathLocation);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RuntimeException("Failed reading fixture: " + classpathLocation);
        }
    }

}
