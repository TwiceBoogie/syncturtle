package com.syncturtle.common.core.text;

import java.util.Optional;
import java.util.Random;

public final class Strings {

    private static final Random RANDOM = new Random();

    public static String nvl(String v, String d) {
        return (v == null || v.isBlank()) ? d : v;
    }

    public static Optional<String> firstNonBlank(String... values) {
        if (values == null) {
            return Optional.empty();
        }
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return Optional.of(v.trim());
            }
        }
        return Optional.empty();
    }

    public static String orEmpty(String v) {
        return v == null ? "" : v;
    }

    public static String randomAsciLetters(String value) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        sb.append(nvl(value, ""));
        for (int i = 0; i < 6; i++) {
            sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
