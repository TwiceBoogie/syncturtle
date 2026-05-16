package com.syncturtle.common.core.security.token;

public interface TokenHasher {
    String hash(String token);
}
