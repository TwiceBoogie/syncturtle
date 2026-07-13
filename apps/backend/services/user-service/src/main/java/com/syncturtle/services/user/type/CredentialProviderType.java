package com.syncturtle.services.user.type;

public enum CredentialProviderType {

    EMAIL_PASSWORD("email"),
    MAGIC_CODE("magic-code"),
    GOOGLE("google"),
    GITHUB("github"),
    GITLAB("gitlab");

    private final String value;

    CredentialProviderType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
