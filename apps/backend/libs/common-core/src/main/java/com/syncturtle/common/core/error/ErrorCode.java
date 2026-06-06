package com.syncturtle.common.core.error;

/**
 * Generic application error-code contract
 * 
 * <p>
 * Domain specific enums such as AuthErrorCode, InstanceErrorCode, etc. should
 * implement this interface.
 */
public interface ErrorCode {
    int getCode();

    /**
     * Stable machine readable key
     * 
     * <p>
     * This is what browser/mobile clients should use for i18n lookup
     * 
     * @return key
     */
    String getKey();

    default int getHttpStatusCode() {
        return 400;
    }

    default String getPublicMessage() {
        return "Request failed.";
    }
}
