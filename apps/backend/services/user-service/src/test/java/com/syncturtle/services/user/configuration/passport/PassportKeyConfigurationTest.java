package com.syncturtle.services.user.configuration.passport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.syncturtle.services.user.configuration.property.AuthProperties;
import com.syncturtle.services.user.configuration.property.PassportProperties;
import com.syncturtle.services.user.controller.JwksController;
import com.syncturtle.services.user.service.collaborator.token.AccessTokenIssuer;
import com.syncturtle.services.user.service.collaborator.token.IssuedAccessTokenReceipt;
import com.syncturtle.services.user.service.param.AccessTokenIssueParam;

class PassportKeyConfigurationTest {

    private static final String ISSUER = "https://api.syncturtle.test/auth";
    private static final String AUDIENCE = "syncturtle-test";
    private static final String KID = "ephemeral-test-kid";

    @TempDir
    Path temporaryDirectory;

    @Nested
    class KeyMaterialBean {

        @Test
        void loadsAnEphemeralRsaKeyPairThroughProductionConfiguration() throws Exception {
            // arrange
            EphemeralRsaKeyPair ephemeralKeyPair = EphemeralRsaKeyPair.generate(
                    temporaryDirectory.resolve("matching"),
                    2048);
            PassportProperties properties = passportProperties(
                    ephemeralKeyPair.privateKeyPath(),
                    ephemeralKeyPair.publicKeyPath());
            PassportKeyConfiguration configuration = new PassportKeyConfiguration();

            // act
            KeyMaterial keyMaterial = configuration.keyMaterial(properties, new DefaultResourceLoader());

            // assert
            assertThat(keyMaterial.publicKey().getModulus())
                    .isEqualTo(ephemeralKeyPair.publicKey().getModulus());
            assertThat(keyMaterial.privateKey().getModulus())
                    .isEqualTo(ephemeralKeyPair.privateKey().getModulus());
        }

        @Test
        void rejectsMismatchedEphemeralPrivateAndPublicKeys() throws Exception {
            // arrange
            EphemeralRsaKeyPair privateKeyPair = EphemeralRsaKeyPair.generate(
                    temporaryDirectory.resolve("private-pair"),
                    2048);
            EphemeralRsaKeyPair publicKeyPair = EphemeralRsaKeyPair.generate(
                    temporaryDirectory.resolve("public-pair"),
                    2048);
            PassportProperties properties = passportProperties(
                    privateKeyPair.privateKeyPath(),
                    publicKeyPair.publicKeyPath());
            PassportKeyConfiguration configuration = new PassportKeyConfiguration();

            // act and assert
            assertThatThrownBy(() -> configuration.keyMaterial(properties, new DefaultResourceLoader()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("do not belong to the same RSA key pair");
        }

        @Test
        void rejectsAnRsaKeyPairSmallerThanTheJoseMinimum() throws Exception {
            // arrange
            EphemeralRsaKeyPair ephemeralKeyPair = EphemeralRsaKeyPair.generate(
                    temporaryDirectory.resolve("undersized"),
                    1024);
            PassportProperties properties = passportProperties(
                    ephemeralKeyPair.privateKeyPath(),
                    ephemeralKeyPair.publicKeyPath());
            PassportKeyConfiguration configuration = new PassportKeyConfiguration();

            // act and assert
            assertThatThrownBy(() -> configuration.keyMaterial(properties, new DefaultResourceLoader()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("at least 2048 bits");
        }
    }

    @Nested
    class JwtAndJwksContract {

        @Test
        void issuesAndPublishesTheSameKidAndEphemeralPublicKey() throws Exception {
            // arrange
            EphemeralRsaKeyPair ephemeralKeyPair = EphemeralRsaKeyPair.generate(
                    temporaryDirectory.resolve("issuer"),
                    2048);
            PassportProperties passportProperties = passportProperties(
                    ephemeralKeyPair.privateKeyPath(),
                    ephemeralKeyPair.publicKeyPath());
            AuthProperties authProperties = new AuthProperties(
                    Duration.ofMinutes(15),
                    Duration.ofMinutes(30),
                    48);
            PassportKeyConfiguration configuration = new PassportKeyConfiguration();
            KeyMaterial keyMaterial = configuration.keyMaterial(passportProperties, new DefaultResourceLoader());
            Algorithm signingAlgorithm = configuration.passportAlgorithm(keyMaterial);
            AccessTokenIssuer issuer = new AccessTokenIssuer(
                    passportProperties,
                    authProperties,
                    keyMaterial,
                    signingAlgorithm);
            JwksController jwksController = new JwksController(issuer);
            AccessTokenIssueParam issueParam = AccessTokenIssueParam.builder()
                    .userId("a1791990-0870-46ee-9b9f-8148a312bc4a")
                    .instanceId("d02e68a1-987a-46d8-b6c0-d884261e643f")
                    .sessionId("bda601cc-6f08-4768-9dd8-7eca3c4fe3ab")
                    .userAuthVersion(1L)
                    .roles(List.of("USER"))
                    .build();

            // act
            IssuedAccessTokenReceipt receipt = issuer.issueAccessToken(issueParam);
            DecodedJWT decodedJwt = JWT.decode(receipt.getToken());
            ResponseEntity<Map<String, Object>> response = jwksController.jwks();

            // assert
            assertThat(decodedJwt.getKeyId()).isEqualTo(KID);
            assertThat(decodedJwt.getAlgorithm()).isEqualTo("RS256");
            JWT.require(Algorithm.RSA256(keyMaterial.publicKey(), null))
                    .withIssuer(ISSUER)
                    .withAudience(AUDIENCE)
                    .build()
                    .verify(receipt.getToken());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> body = response.getBody();
            assertThat(body).isNotNull();
            Object keysValue = body.get("keys");
            assertThat(keysValue).isInstanceOf(List.class);
            List<?> keys = (List<?>) keysValue;
            assertThat(keys).hasSize(1);
            assertThat(keys.getFirst()).isInstanceOf(Map.class);
            Map<?, ?> jwk = (Map<?, ?>) keys.getFirst();

            assertThat(jwk.get("kid")).isEqualTo(KID);
            assertThat(jwk.get("alg")).isEqualTo("RS256");
            assertThat(jwk.get("n")).isEqualTo(toBase64Url(keyMaterial.publicKey().getModulus()));
            assertThat(jwk.get("e")).isEqualTo(toBase64Url(keyMaterial.publicKey().getPublicExponent()));
        }
    }

    private static PassportProperties passportProperties(Path privateKeyPath, Path publicKeyPath) {
        return new PassportProperties(
                ISSUER,
                AUDIENCE,
                KID,
                privateKeyPath.toUri().toString(),
                publicKeyPath.toUri().toString());
    }

    private static String toBase64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] unsigned = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, unsigned, 0, unsigned.length);
            bytes = unsigned;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static final class EphemeralRsaKeyPair {

        private final RSAPublicKey publicKey;
        private final RSAPrivateKey privateKey;
        private final Path publicKeyPath;
        private final Path privateKeyPath;

        private EphemeralRsaKeyPair(
                RSAPublicKey publicKey,
                RSAPrivateKey privateKey,
                Path publicKeyPath,
                Path privateKeyPath) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.publicKeyPath = publicKeyPath;
            this.privateKeyPath = privateKeyPath;
        }

        static EphemeralRsaKeyPair generate(Path directory, int modulusBits) throws Exception {
            Files.createDirectories(directory);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(modulusBits);
            KeyPair keyPair = generator.generateKeyPair();

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            Path publicKeyPath = directory.resolve("jwt-public-key.pem");
            Path privateKeyPath = directory.resolve("jwt-private-key.pem");

            writePem(publicKeyPath, "PUBLIC KEY", publicKey.getEncoded());
            writePem(privateKeyPath, "PRIVATE KEY", privateKey.getEncoded());

            return new EphemeralRsaKeyPair(publicKey, privateKey, publicKeyPath, privateKeyPath);
        }

        RSAPublicKey publicKey() {
            return publicKey;
        }

        RSAPrivateKey privateKey() {
            return privateKey;
        }

        Path publicKeyPath() {
            return publicKeyPath;
        }

        Path privateKeyPath() {
            return privateKeyPath;
        }

        private static void writePem(Path path, String type, byte[] encoded) throws Exception {
            Base64.Encoder encoder = Base64.getMimeEncoder(64, new byte[] { '\n' });
            String body = encoder.encodeToString(encoded);
            String pem = "-----BEGIN " + type + "-----\n"
                    + body
                    + "\n-----END " + type + "-----\n";
            Files.writeString(path, pem, StandardCharsets.US_ASCII);
        }
    }

}
