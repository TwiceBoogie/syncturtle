package com.syncturtle.common.contracts.instance.model;

import java.util.Locale;

public final class InstanceEditions {

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

}
