package com.syncturtle.services.instance.type;

public enum InstanceConfigurationCategory {
    AUTHENTICATION,
    SMTP,
    GOOGLE,
    GITHUB,
    GITLAB,
    INTERCOM,
    ANALYTICS;

    public String value() {
        return name();
    }
}
