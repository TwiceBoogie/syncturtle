package com.syncturtle.common.core.security.token;

public interface SecureTokenGenerator {
    String generateBase64Url(int bytes);
}
