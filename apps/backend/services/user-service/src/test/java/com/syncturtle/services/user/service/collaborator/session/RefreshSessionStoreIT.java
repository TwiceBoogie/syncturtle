package com.syncturtle.services.user.service.collaborator.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.syncturtle.common.cache.property.RedisKeyProperties;
import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;
import com.syncturtle.common.core.security.token.Base64UrlSecureTokenGenerator;
import com.syncturtle.common.core.security.token.Sha256TokenHasher;
import com.syncturtle.services.user.exception.RefreshSessionException;
import com.syncturtle.services.user.service.param.AuthorizedSessionIssueParam;
import com.syncturtle.testing.annotation.UseRedis;

import tools.jackson.databind.json.JsonMapper;

@UseRedis
@DataRedisTest
class RefreshSessionStoreIT {

    private static final String PREFIX = "st:it:refresh-session:";
    private static final String USER = "11111111-1111-1111-1111-111111111111";
    private static final String INSTANCE = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private StringRedisTemplate redis;

    private RefreshSessionRedisKeys keys;
    private RefreshSessionStore store;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        cleanKeys();
        keys = new RefreshSessionRedisKeys(new RedisKeyBuilder(new RedisKeyProperties(PREFIX)));
        UserAuthVersionStore versions = new UserAuthVersionStore(redis, keys);
        store = new RefreshSessionStore(redis, keys, new Base64UrlSecureTokenGenerator(),
                new Sha256TokenHasher(), versions, JsonMapper.builder().findAndAddModules().build(),
                Clock.systemUTC());
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
        cleanKeys();
    }

    @Test
    void creationStoresOneOwnedRecordWithIdleTtlAndNoPlaintextSecret() {
        RefreshSessionReceipt created = store.create(member());
        String json = redis.opsForValue().get(keys.session(created.getSessionId()));
        Long ttl = redis.getExpire(keys.session(created.getSessionId()), TimeUnit.MILLISECONDS);
        RefreshSessionSnapshot presented = store.requirePresented(created.getRefreshToken());

        assertThat(presented.getRecord().getUserId()).isEqualTo(USER);
        assertThat(presented.getRecord().getInstanceId()).isEqualTo(INSTANCE);
        assertThat(json).doesNotContain(created.getRefreshToken());
        assertThat(ttl).isPositive().isLessThanOrEqualTo(Duration.ofDays(7).plusSeconds(5).toMillis());
        assertThat(presented.getRecord().getAbsoluteExpiresAt())
                .isEqualTo(presented.getRecord().getCreatedAt().plus(Duration.ofDays(30)));
        assertThat(redis.opsForValue().get(keys.userVersion(USER))).isEqualTo("4");
    }

    @Test
    void onePresentedSecretCanCreateOnlyOneSuccessor() throws Exception {
        RefreshSessionReceipt created = store.create(member());
        RefreshSessionSnapshot original = store.requirePresented(created.getRefreshToken());
        executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Object> attempt = () -> {
            start.await();
            try {
                return store.rotate(created.getRefreshToken(), original);
            } catch (RefreshSessionException exception) {
                return exception;
            }
        };
        Future<Object> first = executor.submit(attempt);
        Future<Object> second = executor.submit(attempt);
        start.countDown();
        Object firstResult = first.get(5, TimeUnit.SECONDS);
        Object secondResult = second.get(5, TimeUnit.SECONDS);

        assertThat(List.of(firstResult, secondResult).stream().filter(RefreshSessionReceipt.class::isInstance).count())
                .isEqualTo(1);
        assertThat(List.of(firstResult, secondResult).stream().filter(RefreshSessionException.class::isInstance).count())
                .isEqualTo(1);
        assertThat(store.revokePresented(created.getRefreshToken())).isFalse();
        RefreshSessionReceipt winner = firstResult instanceof RefreshSessionReceipt receipt
                ? receipt : (RefreshSessionReceipt) secondResult;
        assertThat(store.requirePresented(winner.getRefreshToken()).getSessionId()).isEqualTo(created.getSessionId());
        assertThat(redis.compareAndDelete(keys.session(created.getSessionId()), original.getExactJson())).isFalse();
    }

    @Test
    void currentLogoutDeletesOnlyThePresentedGeneration() {
        RefreshSessionReceipt created = store.create(member());
        RefreshSessionReceipt rotated = store.rotate(created.getRefreshToken(),
                store.requirePresented(created.getRefreshToken()));

        assertThat(store.revokePresented(created.getRefreshToken())).isFalse();
        assertThat(store.requirePresented(rotated.getRefreshToken())).isNotNull();
        assertThat(store.revokePresented(rotated.getRefreshToken())).isTrue();
        RefreshSessionException rejected = catchThrowableOfType(RefreshSessionException.class,
                () -> store.requirePresented(rotated.getRefreshToken()));
        assertThat(rejected.getReason()).isEqualTo(RefreshSessionException.Reason.INVALID);
    }

    @Test
    void versionChangesAndMalformedRecordsFailClosed() {
        RefreshSessionReceipt created = store.create(member());
        redis.opsForValue().set(keys.userVersion(USER), "5");
        RefreshSessionException stale = catchThrowableOfType(RefreshSessionException.class,
                () -> store.rotate(created.getRefreshToken(), store.requirePresented(created.getRefreshToken())));
        assertThat(stale.getReason()).isEqualTo(RefreshSessionException.Reason.INVALID);

        redis.opsForValue().set(keys.session(created.getSessionId()), "{bad", Duration.ofDays(1));
        RefreshSessionException malformed = catchThrowableOfType(RefreshSessionException.class,
                () -> store.requirePresented(created.getRefreshToken()));
        assertThat(malformed.getReason()).isEqualTo(RefreshSessionException.Reason.INVALID);
    }

    @Test
    void adminVersionCannotBeRestoredByAnOldLogin() {
        RefreshSessionReceipt admin = store.create(admin());
        redis.opsForValue().set(keys.adminVersion(INSTANCE, USER), "9");
        RefreshSessionException stale = catchThrowableOfType(RefreshSessionException.class,
                () -> store.create(admin()));
        assertThat(stale.getReason()).isEqualTo(RefreshSessionException.Reason.INVALID);
        assertThat(redis.opsForValue().get(keys.adminVersion(INSTANCE, USER))).isEqualTo("9");
        assertThat(redis.hasKey(keys.session(admin.getSessionId()))).isTrue();
    }

    @Test
    void userVersionAdvancesMonotonicallyAndInvalidatesOldSessions() {
        RefreshSessionReceipt created = store.create(member());
        UserAuthVersionStore versions = new UserAuthVersionStore(redis, keys);
        versions.advanceUser(USER, 5L);

        assertThat(redis.opsForValue().get(keys.userVersion(USER))).isEqualTo("5");
        RefreshSessionException oldLogin = catchThrowableOfType(RefreshSessionException.class,
                () -> store.create(member()));
        RefreshSessionException oldRefresh = catchThrowableOfType(RefreshSessionException.class,
                () -> store.rotate(created.getRefreshToken(),
                        store.requirePresented(created.getRefreshToken())));
        assertThat(oldLogin.getReason()).isEqualTo(RefreshSessionException.Reason.INVALID);
        assertThat(oldRefresh.getReason()).isEqualTo(RefreshSessionException.Reason.INVALID);
        RefreshSessionException regression = catchThrowableOfType(RefreshSessionException.class,
                () -> versions.advanceUser(USER, 4L));
        assertThat(regression.getReason()).isEqualTo(RefreshSessionException.Reason.INVALID);
        assertThat(redis.opsForValue().get(keys.userVersion(USER))).isEqualTo("5");
    }

    @Test
    void rotationCannotExtendAbsoluteDeadline() {
        RefreshSessionReceipt created = store.create(member());
        RefreshSessionSnapshot original = store.requirePresented(created.getRefreshToken());
        RefreshSessionReceipt next = store.rotate(created.getRefreshToken(), original);
        RefreshSessionRecord before = original.getRecord();
        RefreshSessionRecord after = next.getRecord();
        assertThat(after.getAbsoluteExpiresAt()).isEqualTo(before.getAbsoluteExpiresAt());
        assertThat(after.getIdleExpiresAt()).isAfterOrEqualTo(before.getIdleExpiresAt());
        assertThat(after.getIdleExpiresAt()).isBeforeOrEqualTo(after.getAbsoluteExpiresAt());
    }

    @Test
    void rotationNearAbsoluteDeadlineCapsRedisTtlAtTheFixedDeadline() throws Exception {
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        Instant createdAt = now.minus(Duration.ofDays(29));
        Instant absolute = createdAt.plus(Duration.ofDays(30));
        String sid = "44444444-4444-4444-4444-444444444444";
        String token = sid + "." + "a".repeat(64);
        RefreshSessionRecord record = RefreshSessionRecord.builder()
                .recordVersion(1).userId(USER).instanceId(INSTANCE).roles(List.of("USER"))
                .authVersion(4L).currentRefreshTokenHash(new Sha256TokenHasher().hash(token))
                .createdAt(createdAt).idleExpiresAt(now.plus(Duration.ofHours(1)))
                .absoluteExpiresAt(absolute).build();
        JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
        redis.opsForValue().set(keys.userVersion(USER), "4");
        redis.opsForValue().set(keys.session(sid), mapper.writeValueAsString(record), Duration.ofHours(1));

        RefreshSessionReceipt rotated = store.rotate(token, store.requirePresented(token));
        Long ttl = redis.getExpire(keys.session(sid), TimeUnit.MILLISECONDS);

        assertThat(rotated.getRecord().getIdleExpiresAt()).isEqualTo(absolute);
        assertThat(rotated.getRecord().getAbsoluteExpiresAt()).isEqualTo(absolute);
        assertThat(ttl).isPositive().isLessThanOrEqualTo(Duration.ofDays(1).plusSeconds(5).toMillis());
    }

    @Test
    void missingRecordRejectsPresentedToken() {
        RefreshSessionReceipt created = store.create(member());
        redis.delete(keys.session(created.getSessionId()));

        RefreshSessionException failure = catchThrowableOfType(RefreshSessionException.class,
                () -> store.requirePresented(created.getRefreshToken()));

        assertThat(failure.getReason()).isEqualTo(RefreshSessionException.Reason.INVALID);
    }

    @Test
    void oldRefreshFamilyRecordAtReusedKeyCannotBeReadAsNewSession() {
        String sid = "66666666-6666-6666-6666-666666666666";
        String token = sid + "." + "a".repeat(64);
        RedisKeyBuilder builder = new RedisKeyBuilder(new RedisKeyProperties(PREFIX));
        String legacyKey = builder.authKey("user-service", "refresh-session", "sid", sid);
        redis.opsForValue().set(legacyKey, "{\"recordVersion\":2}", Duration.ofHours(1));

        assertThat(keys.session(sid)).isEqualTo(legacyKey);

        RefreshSessionException failure = catchThrowableOfType(RefreshSessionException.class,
                () -> store.requirePresented(token));

        assertThat(failure.getReason()).isEqualTo(RefreshSessionException.Reason.INVALID);
    }

    private AuthorizedSessionIssueParam member() {
        return AuthorizedSessionIssueParam.builder()
                .userId(USER).instanceId(INSTANCE).roles(List.of("USER")).authVersion(4L).build();
    }

    private AuthorizedSessionIssueParam admin() {
        return AuthorizedSessionIssueParam.builder()
                .userId(USER).instanceId(INSTANCE).roles(List.of("INSTANCE_ADMIN", "USER"))
                .authVersion(4L).adminSessionVersion(8L).build();
    }

    private void cleanKeys() {
        Set<String> found = redis.keys(PREFIX + "*");
        if (found != null && !found.isEmpty()) {
            redis.delete(found);
        }
    }
}
