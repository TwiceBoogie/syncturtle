package com.syncturtle.common.core.utils;

import java.util.Locale;
import java.util.Optional;
import java.util.Random;

import com.syncturtle.common.core.enums.InstanceEdition;

public final class StringHelper {

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

    public static InstanceEdition parseEdition(String raw) {
        if (raw == null || raw.isBlank()) {
            return InstanceEdition.COMMUNITY;
        }
        try {
            return InstanceEdition.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return InstanceEdition.COMMUNITY;
        }
    }

    public static String defaultInstanceName(InstanceEdition edition) {
        return switch (edition) {
            case ENTERPRISE -> "Syncturtle Enterprise";
            case CLOUD -> "Syncturtle Cloud";
            default -> "Syncturtle Community Edition";
        };
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
