package com.syncturtle.services.user.service.collaborator.session;

import static com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures.CLIENT_BINDING;
import static com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures.PRE_AUTH_BINDING;
import static com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures.createParam;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.syncturtle.common.cache.property.RedisKeyProperties;
import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.common.core.security.token.Base64UrlSecureTokenGenerator;
import com.syncturtle.common.core.security.token.Sha256TokenHasher;
import com.syncturtle.services.user.configuration.property.AdminSessionHandoffProperties;
import com.syncturtle.services.user.exception.AdminSessionHandoffException;
import com.syncturtle.services.user.support.clock.MutableClock;
import com.syncturtle.services.user.type.AdminSessionHandoffState;
import com.syncturtle.testing.annotation.UseRedis;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@UseRedis
@DataRedisTest
@DisplayName("AdminSessionHandoffStoreIT")
class AdminSessionHandoffStoreIT {

    private static final String KEY_PREFIX = "st:it:admin-handoff:";
    private static final String OTHER_PRE_AUTH_BINDING = "d".repeat(64);
    private static final String OTHER_CLIENT_BINDING = "e".repeat(64);

    @Autowired
    StringRedisTemplate redis;

    private final List<ExecutorService> executors = new ArrayList<>();
    private final AtomicLong idSequence = new AtomicLong();

    private AdminSessionHandoffRedisKeys keys;
    private AdminSessionHandoffSerializer serializer;
    private AdminSessionHandoffStore store;
    private MutableClock clock;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setup() {
        deleteTestKeys();
        Instant redisAlignedNow = Instant.now().truncatedTo(ChronoUnit.MILLIS).plusMillis(100);
        clock = MutableClock.at(redisAlignedNow, ZoneOffset.UTC);
        jsonMapper = JsonMapper.builder().findAndAddModules().build();
        serializer = new AdminSessionHandoffSerializer(jsonMapper);
        keys = new AdminSessionHandoffRedisKeys(
                new RedisKeyBuilder(new RedisKeyProperties(KEY_PREFIX)));
        AdminSessionHandoffProperties properties = new AdminSessionHandoffProperties(Duration.ofSeconds(30));
        store = new AdminSessionHandoffStore(
                redis,
                keys,
                new Base64UrlSecureTokenGenerator(),
                new Sha256TokenHasher(),
                serializer,
                new AdminSessionHandoffScriptExecutor(redis),
                properties,
                clock,
                this::nextId);
    }

    @AfterEach
    void cleanup() {
        for (ExecutorService executor : executors) {
            executor.shutdownNow();
        }
        deleteTestKeys();
    }

    @Nested
    @DisplayName("create(param)")
    class CreateTests {

        @Test
        @DisplayName("stores only hashed bounded pending state with a fixed TTL")
        void storesOnlyHashedBoundedPendingState() {
            // arrange
            // act
            AdminSessionHandoffReceipt receipt = store.create(createParam());
            // assert
            String receiptId = receiptId(receipt.getCompletionCode());
            String secret = secret(receipt.getCompletionCode());
            String json = redis.opsForValue().get(keys.handoff(receiptId));
            AdminSessionHandoffRecord record = serializer.decode(json);
            assertThat(record.getState()).isEqualTo(AdminSessionHandoffState.PENDING);
            assertThat(record.getClaimId()).isNull();
            assertThat(record.getExpiresAtEpochMilli() - record.getIssuedAtEpochMilli()).isEqualTo(30_000L);
            assertThat(json)
                    .doesNotContain(receipt.getCompletionCode(), secret, "192.0.2.10", "Admin browser")
                    .doesNotContain("accessToken", "refreshToken", "signedCsrfToken");
            Long ttl = redis.getExpire(keys.handoff(receiptId), TimeUnit.MILLISECONDS);
            assertThat(ttl).isPositive().isLessThanOrEqualTo(30_000L);
        }

        @Test
        @DisplayName("fails closed when Redis is unavailable")
        void failsClosedWhenRedisIsUnavailable() {
            // arrange
            LettuceConnectionFactory unavailableFactory = new LettuceConnectionFactory(
                    new RedisStandaloneConfiguration("127.0.0.1", 1));
            unavailableFactory.afterPropertiesSet();
            StringRedisTemplate unavailableRedis = new StringRedisTemplate(unavailableFactory);
            unavailableRedis.afterPropertiesSet();
            AdminSessionHandoffStore unavailableStore = new AdminSessionHandoffStore(
                    unavailableRedis,
                    keys,
                    new Base64UrlSecureTokenGenerator(),
                    new Sha256TokenHasher(),
                    serializer,
                    new AdminSessionHandoffScriptExecutor(unavailableRedis),
                    new AdminSessionHandoffProperties(Duration.ofSeconds(30)),
                    clock,
                    this::unavailableId);
            // act
            AdminSessionHandoffException failure;
            try {
                failure = catchThrowableOfType(
                        AdminSessionHandoffException.class,
                        () -> unavailableStore.create(createParam()));
            } finally {
                unavailableFactory.destroy();
            }
            // assert
            assertThat(failure.getReason()).isEqualTo(AdminSessionHandoffException.Reason.REDIS_UNAVAILABLE);
        }

        private String unavailableId() {
            return "99999999-9999-9999-9999-999999999999";
        }

    }

    @Nested
    @DisplayName("claim(code, bindings)")
    class ClaimTests {

        @Test
        @DisplayName("linearizes two concurrent claimants so exactly one wins")
        void linearizesConcurrentClaimants() throws Exception {
            // arrange
            AdminSessionHandoffReceipt receipt = store.create(createParam());
            CyclicBarrier barrier = new CyclicBarrier(2);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            executors.add(executor);
            // conditions
            Future<String> first = executor.submit(
                    () -> claimOutcome(barrier, receipt.getCompletionCode()));
            Future<String> second = executor.submit(
                    () -> claimOutcome(barrier, receipt.getCompletionCode()));
            // act
            List<String> outcomes = List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
            // assert
            assertThat(outcomes).containsExactlyInAnyOrder("CLAIMED", "REPLAYED");
        }

        @Test
        @DisplayName("wrong secret or either binding fails without extending TTL")
        void rejectsWrongSecretAndBindingsWithoutExtendingTtl() {
            // arrange
            AdminSessionHandoffReceipt wrongSecret = store.create(createParam());
            AdminSessionHandoffReceipt wrongPreAuth = store.create(createParam());
            AdminSessionHandoffReceipt wrongClient = store.create(createParam());
            String wrongSecretKey = keys.handoff(receiptId(wrongSecret.getCompletionCode()));
            Long ttlBefore = redis.getExpire(wrongSecretKey, TimeUnit.MILLISECONDS);
            // act
            AdminSessionHandoffException secretFailure = catchThrowableOfType(
                    AdminSessionHandoffException.class,
                    () -> store.claim(
                            receiptId(wrongSecret.getCompletionCode()) + ".wrong-secret",
                            PRE_AUTH_BINDING,
                            CLIENT_BINDING));
            AdminSessionHandoffException preAuthFailure = catchThrowableOfType(
                    AdminSessionHandoffException.class,
                    () -> store.claim(wrongPreAuth.getCompletionCode(), OTHER_PRE_AUTH_BINDING, CLIENT_BINDING));
            AdminSessionHandoffException clientFailure = catchThrowableOfType(
                    AdminSessionHandoffException.class,
                    () -> store.claim(wrongClient.getCompletionCode(), PRE_AUTH_BINDING, OTHER_CLIENT_BINDING));
            // assert
            assertThat(secretFailure.getReason()).isEqualTo(AdminSessionHandoffException.Reason.INVALID);
            assertThat(preAuthFailure.getReason()).isEqualTo(AdminSessionHandoffException.Reason.BINDING_MISMATCH);
            assertThat(clientFailure.getReason()).isEqualTo(AdminSessionHandoffException.Reason.BINDING_MISMATCH);
            Long ttlAfter = redis.getExpire(wrongSecretKey, TimeUnit.MILLISECONDS);
            assertThat(ttlAfter).isPositive().isLessThanOrEqualTo(ttlBefore);
        }

        @Test
        @DisplayName("application expiry deletes and rejects without sleeping")
        void applicationExpiryDeletesAndRejectsWithoutSleeping() {
            // arrange
            AdminSessionHandoffReceipt receipt = store.create(createParam());
            String key = keys.handoff(receiptId(receipt.getCompletionCode()));
            clock.advance(Duration.ofSeconds(30));
            // act
            AdminSessionHandoffException failure = catchThrowableOfType(
                    AdminSessionHandoffException.class,
                    () -> store.claim(receipt.getCompletionCode(), PRE_AUTH_BINDING, CLIENT_BINDING));
            // assert
            assertThat(failure.getReason()).isEqualTo(AdminSessionHandoffException.Reason.EXPIRED);
            assertThat(redis.hasKey(key)).isFalse();
        }

        @Test
        @DisplayName("malformed, unsupported, and wrong-type state fail closed")
        void rejectsInvalidRedisState() throws Exception {
            // arrange
            AdminSessionHandoffReceipt malformed = store.create(createParam());
            AdminSessionHandoffReceipt unsupported = store.create(createParam());
            AdminSessionHandoffReceipt wrongType = store.create(createParam());
            redis.opsForValue().set(keys.handoff(receiptId(malformed.getCompletionCode())), "{}");
            String unsupportedKey = keys.handoff(receiptId(unsupported.getCompletionCode()));
            ObjectNode unsupportedJson = (ObjectNode) jsonMapper.readTree(redis.opsForValue().get(unsupportedKey));
            unsupportedJson.put("recordVersion", 2);
            redis.opsForValue().set(unsupportedKey, unsupportedJson.toString(), Duration.ofSeconds(30));
            String wrongTypeKey = keys.handoff(receiptId(wrongType.getCompletionCode()));
            redis.delete(wrongTypeKey);
            redis.opsForList().rightPush(wrongTypeKey, "wrong-type");
            // act
            AdminSessionHandoffException malformedFailure = catchThrowableOfType(
                    AdminSessionHandoffException.class,
                    () -> store.claim(malformed.getCompletionCode(), PRE_AUTH_BINDING, CLIENT_BINDING));
            AdminSessionHandoffException unsupportedFailure = catchThrowableOfType(
                    AdminSessionHandoffException.class,
                    () -> store.claim(unsupported.getCompletionCode(), PRE_AUTH_BINDING, CLIENT_BINDING));
            AdminSessionHandoffException wrongTypeFailure = catchThrowableOfType(
                    AdminSessionHandoffException.class,
                    () -> store.claim(wrongType.getCompletionCode(), PRE_AUTH_BINDING, CLIENT_BINDING));
            // assert
            assertThat(malformedFailure.getReason())
                    .isEqualTo(AdminSessionHandoffException.Reason.MALFORMED_RECORD);
            assertThat(unsupportedFailure.getReason())
                    .isEqualTo(AdminSessionHandoffException.Reason.UNSUPPORTED_RECORD_VERSION);
            assertThat(wrongTypeFailure.getReason())
                    .isEqualTo(AdminSessionHandoffException.Reason.WRONG_REDIS_TYPE);
        }

        private String claimOutcome(CyclicBarrier barrier, String completionCode) throws Exception {
            barrier.await(5, TimeUnit.SECONDS);
            try {
                store.claim(completionCode, PRE_AUTH_BINDING, CLIENT_BINDING);
                return "CLAIMED";
            } catch (AdminSessionHandoffException exception) {
                return exception.getReason().name();
            }
        }

    }

    @Nested
    @DisplayName("release/consume")
    class ReleaseConsumeTests {

        @Test
        @DisplayName("release restores pending with the original bounded TTL and permits one new claim")
        void releaseRestoresPendingWithoutExtendingTtl() {
            // arrange
            AdminSessionHandoffReceipt receipt = store.create(createParam());
            AdminSessionHandoffClaim first = store.claim(
                    receipt.getCompletionCode(), PRE_AUTH_BINDING, CLIENT_BINDING);
            String key = keys.handoff(receiptId(receipt.getCompletionCode()));
            Long ttlBefore = redis.getExpire(key, TimeUnit.MILLISECONDS);
            // act
            store.release(first);
            Long ttlAfter = redis.getExpire(key, TimeUnit.MILLISECONDS);
            AdminSessionHandoffClaim second = store.claim(
                    receipt.getCompletionCode(), PRE_AUTH_BINDING, CLIENT_BINDING);
            // assert
            assertThat(ttlAfter).isPositive().isLessThanOrEqualTo(ttlBefore);
            assertThat(second.getClaimId()).isNotEqualTo(first.getClaimId());
        }

        @Test
        @DisplayName("consume deletes matching claim and replay cannot issue another claim")
        void consumeDeletesAndRejectsReplay() {
            // arrange
            AdminSessionHandoffReceipt receipt = store.create(createParam());
            AdminSessionHandoffClaim claimed = store.claim(
                    receipt.getCompletionCode(), PRE_AUTH_BINDING, CLIENT_BINDING);
            String key = keys.handoff(receiptId(receipt.getCompletionCode()));
            // act
            store.consume(claimed);
            AdminSessionHandoffException replay = catchThrowableOfType(
                    AdminSessionHandoffException.class,
                    () -> store.claim(receipt.getCompletionCode(), PRE_AUTH_BINDING, CLIENT_BINDING));
            // assert
            assertThat(redis.hasKey(key)).isFalse();
            assertThat(replay.getReason()).isEqualTo(AdminSessionHandoffException.Reason.INVALID);
        }

        @Test
        @DisplayName("script execution recovers after the Redis script cache is flushed")
        void recoversAfterScriptCacheFlush() {
            // arrange
            AdminSessionHandoffReceipt receipt = store.create(createParam());
            redis.execute((RedisCallback<Void>) connection -> {
                connection.scriptingCommands().scriptFlush();
                return null;
            });
            // act
            AdminSessionHandoffClaim claim = store.claim(
                    receipt.getCompletionCode(), PRE_AUTH_BINDING, CLIENT_BINDING);
            store.consume(claim);
            // assert
            assertThat(redis.hasKey(keys.handoff(receiptId(receipt.getCompletionCode())))).isFalse();
        }

    }

    private String nextId() {
        return String.format("00000000-0000-0000-0000-%012d", idSequence.incrementAndGet());
    }

    private static String receiptId(String completionCode) {
        return completionCode.substring(0, completionCode.indexOf('.'));
    }

    private static String secret(String completionCode) {
        return completionCode.substring(completionCode.indexOf('.') + 1);
    }

    private void deleteTestKeys() {
        Set<String> testKeys = redis.keys(KEY_PREFIX + "*");
        if (testKeys != null && !testKeys.isEmpty()) {
            redis.delete(testKeys);
        }
    }

}
