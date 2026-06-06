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
     * @return key
     */
    String getKey();
}
