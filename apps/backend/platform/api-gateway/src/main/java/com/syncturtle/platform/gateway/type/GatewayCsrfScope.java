package com.syncturtle.platform.gateway.type;

public enum GatewayCsrfScope {
    PREAUTH("P"),
    SESSION("S");

    private final String claimValue;

    GatewayCsrfScope(String claimValue) {
        this.claimValue = claimValue;
    }

    public String getClaimValue() {
        return claimValue;
    }
}
