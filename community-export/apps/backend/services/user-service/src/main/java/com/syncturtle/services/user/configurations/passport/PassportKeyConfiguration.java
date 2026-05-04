package com.syncturtle.services.user.configurations.passport;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import com.auth0.jwt.algorithms.Algorithm;
import com.syncturtle.services.user.configurations.properties.KeyMaterial;
import com.syncturtle.services.user.configurations.properties.PassportProperties;

@Configuration
public class PassportKeyConfiguration {

    @Bean
    KeyMaterial keyMaterial(PassportProperties properties, ResourceLoader resourceLoader) {
        RSAPublicKeyReader reader = new RSAPublicKeyReader(resourceLoader);
        return new KeyMaterial(
                reader.readPublicKey(properties.getPublicKeyLocation()),
                reader.readPrivateKey(properties.getPrivateKeyLocation()));
    }

    @Bean
    Algorithm passportAlgorithm(KeyMaterial keyMaterial) {
        return Algorithm.RSA256(
                keyMaterial.publicKey(),
                keyMaterial.privateKey());
    }

}
