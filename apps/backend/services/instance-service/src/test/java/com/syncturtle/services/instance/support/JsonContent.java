package com.syncturtle.services.instance.support;

public final class JsonContent {

    private JsonContent() {
    }

    public static String object(String body) {
        return "{" + body + "}";
    }

    public static String instanceUpdate(String instanceName, Boolean telemetryEnabled) {
        StringBuilder json = new StringBuilder("{");
        boolean hasPrevious = false;

        if (instanceName != null) {
            json.append("\"instanceName\":\"").append(escape(instanceName)).append("\"");
            hasPrevious = true;
        }

        if (telemetryEnabled != null) {
            if (hasPrevious) {
                json.append(",");
            }
            json.append("\"telemetryEnabled\":").append(telemetryEnabled);
        }

        json.append("}");
        return json.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
