package com.syncturtle.services.user.configuration.passport;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import com.auth0.jwt.algorithms.Algorithm;
import com.syncturtle.services.user.configuration.property.PassportProperties;
import com.syncturtle.services.user.security.key.RsaPublicKeyReader;

@Configuration
public class PassportKeyConfiguration {

    private static final int MINIMUM_RSA_MODULUS_BITS = 2048;

    @Bean
    KeyMaterial keyMaterial(PassportProperties properties, ResourceLoader resourceLoader) {
        RsaPublicKeyReader reader = new RsaPublicKeyReader(resourceLoader);
        RSAPublicKey publicKey = reader.readPublicKey(properties.getPublicKeyLocation());
        RSAPrivateKey privateKey = reader.readPrivateKey(properties.getPrivateKeyLocation());

        validateKeyPair(publicKey, privateKey);

        return new KeyMaterial(publicKey, privateKey);
    }

    @Bean
    Algorithm passportAlgorithm(KeyMaterial keyMaterial) {
        return Algorithm.RSA256(
                keyMaterial.publicKey(),
                keyMaterial.privateKey());
    }

    private static void validateKeyPair(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        if (publicKey.getModulus().bitLength() < MINIMUM_RSA_MODULUS_BITS
                || privateKey.getModulus().bitLength() < MINIMUM_RSA_MODULUS_BITS) {
            throw new IllegalStateException(
                    "Passport RSA signing keys must use a modulus of at least "
                            + MINIMUM_RSA_MODULUS_BITS
                            + " bits");
        }

        if (!publicKey.getModulus().equals(privateKey.getModulus())) {
            throw new IllegalStateException("Passport private and public keys do not belong to the same RSA key pair");
        }
    }

}
