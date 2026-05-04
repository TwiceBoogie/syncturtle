package com.syncturtle.services.instance.configurations.crypto;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.services.instance.configurations.properties.InstanceServiceProperties;

@Configuration(proxyBeanMethods = false)
public class JasyptConfig {

    @Bean
    StringEncryptor stringEncryptor(InstanceServiceProperties props) {
        String secretKey = props.getSecurity().getEncryption().getSecretKey();

        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("APP_SECURITY_ENCRYPTION_SECRET_KEY must be set");
        }

        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(secretKey);
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("10000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        ;
        config.setStringOutputType("base64");

        encryptor.setConfig(config);
        return encryptor;
    }
}
