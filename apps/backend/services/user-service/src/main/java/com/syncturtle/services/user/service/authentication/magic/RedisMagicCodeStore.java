package com.syncturtle.services.user.service.authentication.magic;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Component
@RequiredArgsConstructor
public class RedisMagicCodeStore implements MagicCodeStore {

    private static final Duration EXPIRATION = Duration.ofMinutes(10);
    private static final int MAX_STORED_ATTEMPTS = 3;
    private static final char[] LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public MagicCodeChallenge createOrRotate(String email, AuthErrorCode attemptExhaustedErrorCode) {
        Assert.notNull(attemptExhaustedErrorCode, "errorCode is required");

        String normalizedEmail = normalizeEmail(email);
        String redisKey = redisKey(normalizedEmail);
        String token = generateDashedCode();

        MagicCodePayload payload = readPayload(redisKey);

        if (payload != null && payload.getCurrentAttempt() >= MAX_STORED_ATTEMPTS) {
            throw AuthException.of(attemptExhaustedErrorCode).with("email", normalizedEmail);
        }

        int nextAttempt = payload == null ? 0 : payload.getCurrentAttempt() + 1;

        MagicCodePayload nextPayload = new MagicCodePayload();
        nextPayload.setCurrentAttempt(nextAttempt);
        nextPayload.setEmail(normalizedEmail);
        nextPayload.setToken(token);

        writePayload(redisKey, nextPayload);

        return new MagicCodeChallenge(redisKey, token);
    }

    @Override
    public VerificationResult verify(String email, String submittedCode) {
        Assert.hasText(submittedCode, "submittedCode is required");

        String normalizedEmail = normalizeEmail(email);
        String redisKey = redisKey(normalizedEmail);

        MagicCodePayload payload = readPayload(redisKey);

        if (payload == null) {
            return VerificationResult.failed(FailureReason.EXPIRED);
        }

        if (!payload.getToken().equals(submittedCode.trim())) {
            return VerificationResult.failed(FailureReason.INVALID);
        }

        redis.delete(redisKey);

        return VerificationResult.verified();
    }

    private MagicCodePayload readPayload(String redisKey) {
        String json = redis.opsForValue().get(redisKey);

        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, MagicCodePayload.class);
        } catch (Exception exception) {
            redis.delete(redisKey);
            return null;
        }
    }

    private void writePayload(String redisKey, MagicCodePayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redis.opsForValue().set(redisKey, json, EXPIRATION);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to store magic code payload", exception);
        }
    }

    private String redisKey(String email) {
        return "magic_" + email.trim().toLowerCase();
    }

    private String generateDashedCode() {
        return randomLetters(4) + "-" + randomLetters(4) + "-" + randomLetters(4);
    }

    private String randomLetters(int length) {
        StringBuilder builder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            builder.append(LOWERCASE_LETTERS[secureRandom.nextInt(LOWERCASE_LETTERS.length)]);
        }

        return builder.toString();
    }

    private static String normalizeEmail(String email) {
        Assert.hasText(email, "email is required");

        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Getter
    @Setter
    public static final class MagicCodePayload {
        private int currentAttempt;
        private String email;
        private String token;
    }

}
