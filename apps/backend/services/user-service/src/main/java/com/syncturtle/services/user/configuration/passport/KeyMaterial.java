package com.syncturtle.services.user.configuration.passport;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public record KeyMaterial(
        RSAPublicKey publicKey,
        RSAPrivateKey privateKey) {
}
