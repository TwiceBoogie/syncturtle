package com.syncturtle.services.user.service.authentication.magic;

import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Component
@RequiredArgsConstructor
public class RedisMagicCodeStore implements MagicCodeStore {

    private static final Duration EXPIRATION = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 3;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public MagicCodeChallenge createOrRotate(String email) {
        Assert.hasText(email, "email is required");

        String normalizedEmail = email.trim().toLowerCase();
        String redisKey = redisKey(normalizedEmail);
        String code = generateSixDigitCode();

        MagicCodePayload payload = readPayload(redisKey);

        if (payload != null && payload.getCurrentAttempt() >= MAX_ATTEMPTS) {
            throw new MagicCodeAttemptExhaustedException(normalizedEmail);
        }

        int nextAttempt = payload == null ? 0 : payload.getCurrentAttempt() + 1;

        MagicCodePayload nextPayload = new MagicCodePayload();
        nextPayload.setCurrentAttempt(nextAttempt);
        nextPayload.setEmail(normalizedEmail);
        nextPayload.setCode(code);

        writePayload(redisKey, nextPayload);

        return new MagicCodeChallenge(redisKey, code);
    }

    @Override
    public VerificationResult verify(String email, String submittedCode) {
        Assert.hasText(email, "email is required");
        Assert.hasText(submittedCode, "submittedCode is required");

        String normalizedEmail = email.trim().toLowerCase();
        String redisKey = redisKey(normalizedEmail);

        MagicCodePayload payload = readPayload(redisKey);

        if (payload == null) {
            return VerificationResult.failed(FailureReason.EXPIRED);
        }

        if (!payload.getCode().equals(submittedCode.trim())) {
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

    private String generateSixDigitCode() {
        int value = secureRandom.nextInt(900_000) + 100_000;
        return Integer.toString(value);
    }

    private String redisKey(String email) {
        return "magic_" + email.trim().toLowerCase();
    }

    @Getter
    @Setter
    public static final class MagicCodePayload {
        private int currentAttempt;
        private String email;
        private String code;
    }

    public static final class MagicCodeAttemptExhaustedException extends RuntimeException {
        private final String email;

        public MagicCodeAttemptExhaustedException(String email) {
            super("Magic code attempt exhausted");
            this.email = email;
        }

        public String getEmail() {
            return email;
        }
    }

}
