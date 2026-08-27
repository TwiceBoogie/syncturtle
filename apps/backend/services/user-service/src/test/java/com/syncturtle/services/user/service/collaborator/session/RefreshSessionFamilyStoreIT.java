package com.syncturtle.services.user.service.collaborator.session;

import static com.syncturtle.services.user.support.fixture.RefreshSessionPropertyFixtures.lifecycleProperties;
import static com.syncturtle.services.user.support.fixture.RefreshSessionPropertyFixtures.successorEnvelopeProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.security.SecureRandom;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

import com.syncturtle.common.cache.property.RedisKeyProperties;
import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.common.core.security.token.Base64UrlSecureTokenGenerator;
import com.syncturtle.common.core.security.token.Sha256TokenHasher;
import com.syncturtle.services.user.configuration.property.RefreshSessionLifecycleProperties;
import com.syncturtle.services.user.exception.RefreshSessionLifecycleException;
import com.syncturtle.services.user.service.param.RefreshSessionFamilyCreateParam;
import com.syncturtle.services.user.service.param.RefreshSessionFamilyRotateParam;
import com.syncturtle.services.user.support.clock.MutableClock;
import com.syncturtle.services.user.type.RefreshSessionLifecycleOutcome;
import com.syncturtle.testing.annotation.UseRedis;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@UseRedis
@DataRedisTest
@DisplayName("RefreshSessionFamilyStoreIT")
class RefreshSessionFamilyStoreIT {

    private static final String KEY_PREFIX = "st:it:refresh-family:";
    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String FOREIGN_USER_ID = "33333333-3333-3333-3333-333333333333";
    private static final String INSTANCE_ID = "22222222-2222-2222-2222-222222222222";
    private static final String CLIENT_BINDING = "a".repeat(64);
    private static final String OTHER_CLIENT_BINDING = "b".repeat(64);

    @Autowired
    private StringRedisTemplate redis;

    private final List<ExecutorService> executors = new ArrayList<>();

    private RefreshSessionRedisKeys keys;
    private RefreshSessionFamilySerializer serializer;
    private JsonMapper jsonMapper;
    private MutableClock clock;

    private RefreshSessionFamilyStore store;

    @BeforeEach
    void setup() {
        deleteProposalTestKeys();
        Instant redisAlignedNow = Instant.now().truncatedTo(ChronoUnit.MILLIS).plusMillis(100);
        clock = MutableClock.at(redisAlignedNow, ZoneOffset.UTC);
        jsonMapper = JsonMapper.builder().findAndAddModules().build();
        serializer = new RefreshSessionFamilySerializer(jsonMapper);

        RedisKeyBuilder keyBuilder = new RedisKeyBuilder(new RedisKeyProperties(KEY_PREFIX));
        keys = new RefreshSessionRedisKeys(keyBuilder);
        RefreshSessionLifecycleProperties lifecycleProperties = lifecycleProperties();

        RefreshSessionSuccessorEnvelopeCipher envelopeCipher = new RefreshSessionSuccessorEnvelopeCipher(
                successorEnvelopeProperties(), new SecureRandom());
        store = new RefreshSessionFamilyStore(
                redis,
                keys,
                new Base64UrlSecureTokenGenerator(),
                new Sha256TokenHasher(),
                serializer,
                new RefreshSessionLifetimeDecider(lifecycleProperties, clock),
                envelopeCipher,
                new RefreshSessionLifecycleScriptExecutor(redis),
                lifecycleProperties,
                jsonMapper);
    }

    @AfterEach
    void cleanup() {
        for (ExecutorService executor : executors) {
            executor.shutdownNow();
        }
        deleteProposalTestKeys();
    }

    @Nested
    @DisplayName("create(RefreshSessionFamilyCreateParam)")
    class CreateTests {

        @Test
        @DisplayName("creates one family and maintains authoritative state atomically")
        void createsOneFamilyAndMaintainsAuthoritativeStateAtomically() {
            // arrange
            RefreshSessionFamilyCreateParam param = baseCreateParam("browser");
            // conditions
            // act
            RefreshSessionLifecycleResult result = store.create(param);
            // assert
            String familyJson = redis.opsForValue().get(keys.session(result.getSessionId()));
            RefreshSessionFamilyRecord family = serializer.decode(familyJson);
            assertThat(result.getOutcome()).isEqualTo(RefreshSessionLifecycleOutcome.CREATED);
            assertThat(result.getRefreshToken()).startsWith(result.getSessionId() + ".");
            assertThat(family.getRecordVersion()).isEqualTo(2);
            assertThat(family.getRotationCounter()).isZero();
            assertThat(family.getCreatedAt()).isEqualTo(family.getLastUsedAt());
            assertThat(family.getAbsoluteExpiresAt()).isEqualTo(family.getCreatedAt().plus(Duration.ofDays(30)));
            assertThat(redis.opsForZSet().score(keys.baseIndex(USER_ID), result.getSessionId()))
                    .isEqualTo((double) family.getLastUsedAt().toEpochMilli());
            assertThat(redis.opsForZSet().size(keys.elevatedIndex(USER_ID))).isZero();
            assertThat(redis.opsForValue().get(keys.userAuthVersion(USER_ID))).isEqualTo("7");
            assertThat(familyJson).doesNotContain(result.getRefreshToken());

            Long ttl = redis.getExpire(keys.session(result.getSessionId()), TimeUnit.MILLISECONDS);
            assertThat(ttl).isPositive().isLessThanOrEqualTo(Duration.ofDays(7).plusSeconds(1).toMillis());
        }

        @Test
        @DisplayName("eleventh base family evicts least recently used non current family")
        void eleventhBaseFamilyEvictsLeastRecentlyUsedNonCurrentFamily() {
            // arrange
            List<RefreshSessionLifecycleResult> existing = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                existing.add(store.create(baseCreateParam("browser-" + i)));
                clock.advance(Duration.ofSeconds(1));
            }
            String leastRecentlyUsedSid = existing.get(0).getSessionId();
            // conditions
            // act
            RefreshSessionLifecycleResult result = store.create(baseCreateParam("browser-10"));
            // assert
            assertThat(redis.opsForZSet().size(keys.baseIndex(USER_ID))).isEqualTo(10L);
            assertThat(redis.hasKey(keys.session(leastRecentlyUsedSid))).isFalse();
            assertThat(redis.hasKey(keys.grace(leastRecentlyUsedSid))).isFalse();
            assertThat(redis.hasKey(keys.session(result.getSessionId()))).isTrue();
            assertThat(redis.opsForZSet().score(keys.baseIndex(USER_ID), result.getSessionId())).isNotNull();
            // verify
        }

        @Test
        @DisplayName("fourth elevated family evicts lease recently used elevated family")
        void fourthElevatedFamilyEvictsLeastRecentlyUsedElevatedFamily() {
            // arrange
            List<RefreshSessionLifecycleResult> existing = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                existing.add(store.create(adminCreateParam("admin-" + i)));
                clock.advance(Duration.ofSeconds(1));
            }
            String leastRecentlyUsedSid = existing.get(0).getSessionId();
            // act
            RefreshSessionLifecycleResult result = store.create(adminCreateParam("admin-3"));
            // assert
            assertThat(redis.opsForZSet().size(keys.elevatedIndex(USER_ID))).isEqualTo(3L);
            assertThat(redis.opsForZSet().size(keys.baseIndex(USER_ID))).isEqualTo(3L);
            assertThat(redis.hasKey(keys.session(leastRecentlyUsedSid))).isFalse();
            assertThat(redis.opsForZSet().score(keys.elevatedIndex(USER_ID), result.getSessionId())).isNotNull();
            // verify
        }

        @Test
        @DisplayName("wrong index type rejects without creating partial family")
        void wrongIndexTypeRejectsWithoutCreatingPartialFamily() {
            // arrange
            redis.opsForValue().set(keys.baseIndex(USER_ID), "wrong-type");
            Set<String> keysBefore = redis.keys(KEY_PREFIX + "*");
            // conditions
            // act
            RefreshSessionLifecycleException failure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.create(baseCreateParam("browser")));
            // assert
            assertThat(failure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.WRONG_REDIS_TYPE);
            assertThat(redis.keys(KEY_PREFIX + "*")).containsExactlyInAnyOrderElementsOf(keysBefore);
            assertThat(redis.opsForValue().get(keys.baseIndex(USER_ID))).isEqualTo("wrong-type");
            // verify
        }

    }

    @Nested
    @DisplayName("rotate(RefreshSessionFamilyRotateParam)")
    class RotateTests {

        @Test
        @DisplayName("rotates current token without changing family or absolute boundary")
        void rotatesCurrentTokenWithoutChangingFamilyOrAbsoluteBoundary() {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));
            Instant createdAt = created.getFamily().getCreatedAt();
            Instant absoluteExpiresAt = created.getFamily().getAbsoluteExpiresAt();
            clock.advance(Duration.ofDays(2));
            // conditions
            // act
            RefreshSessionLifecycleResult result = store.rotate(baseRotateParam(created.getRefreshToken()));
            // assert
            assertThat(result.getOutcome()).isEqualTo(RefreshSessionLifecycleOutcome.ROTATED);
            assertThat(result.getSessionId()).isEqualTo(created.getSessionId());
            assertThat(result.getRefreshToken()).isNotEqualTo(created.getRefreshToken());
            assertThat(result.getFamily().getRotationCounter()).isEqualTo(1L);
            assertThat(result.getFamily().getCreatedAt()).isEqualTo(createdAt);
            assertThat(result.getFamily().getAbsoluteExpiresAt()).isEqualTo(absoluteExpiresAt);
            assertThat(result.getFamily().getLastUsedAt()).isEqualTo(clock.instant());
            assertThat(redis.opsForZSet().size(keys.baseIndex(USER_ID))).isEqualTo(1L);
            assertThat(redis.hasKey(keys.grace(created.getSessionId()))).isTrue();
            // verify
        }

        @Test
        @DisplayName("backward app time cannot extend activity or deadline")
        void backwardApplicationTimeCannotExtendActivityOrDeadline() {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));
            clock.advance(Duration.ofDays(2));
            RefreshSessionLifecycleResult firstRotation = store.rotate(baseRotateParam(created.getRefreshToken()));
            Instant lastUsedAt = firstRotation.getFamily().getLastUsedAt();
            Instant idleExpiresAt = firstRotation.getFamily().getIdleExpiresAt();
            clock.rewind(Duration.ofDays(1));
            // conditions
            // act
            RefreshSessionLifecycleResult result = store.rotate(baseRotateParam(firstRotation.getRefreshToken()));
            // assert
            assertThat(result.getFamily().getLastUsedAt()).isEqualTo(lastUsedAt);
            assertThat(result.getFamily().getIdleExpiresAt()).isEqualTo(idleExpiresAt);
            assertThat(result.getFamily().getCreatedAt()).isEqualTo(created.getFamily().getCreatedAt());
            // verify
        }

        @Test
        @DisplayName("idle boundary deletes family and indexes")
        void idleBoundaryDeletesFamilyAndIndexes() {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));
            clock.advance(Duration.ofDays(7));
            // conditions
            // act
            RefreshSessionLifecycleException failure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.rotate(baseRotateParam(created.getRefreshToken())));
            // assert
            assertThat(failure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.EXPIRED_FAMILY);
            assertFamilyCompletelyAbsent(created.getSessionId());
            // verify
        }

        @Test
        @DisplayName("absolute boundary deletes family even after sliding idle activity")
        void absoluteBoundaryDeletesFamilyEvenAfterSlidingIdleActivity() {
            // arrange
            RefreshSessionLifecycleResult current = store.create(baseCreateParam("browser"));
            for (int i = 0; i < 4; i++) {
                clock.advance(Duration.ofDays(6));
                current = store.rotate(baseRotateParam(current.getRefreshToken()));
            }
            clock.advance(Duration.ofDays(6));
            String tokenAtBoundary = current.getRefreshToken();
            String sessionId = current.getSessionId();
            // conditions
            // act
            RefreshSessionLifecycleException failure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.rotate(baseRotateParam(tokenAtBoundary)));
            // assert
            assertThat(failure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.EXPIRED_FAMILY);
            assertFamilyCompletelyAbsent(sessionId);
            // verify
        }

        @Test
        @DisplayName("rotation into elevated classification enforces three family capacity")
        void rotationIntoElevatedClassificationEnforcesThreeFamilyCapacity() {
            // arrange
            RefreshSessionLifecycleResult firstAdmin = store.create(adminCreateParam("admin-0"));
            clock.advance(Duration.ofSeconds(1));
            store.create(adminCreateParam("admin-1"));
            clock.advance(Duration.ofSeconds(1));
            store.create(adminCreateParam("admin-2"));
            clock.advance(Duration.ofSeconds(1));
            RefreshSessionLifecycleResult normal = store.create(baseCreateParam("browser"));
            // conditions
            // act
            RefreshSessionLifecycleResult result = store.rotate(adminRotateParam(normal.getRefreshToken()));
            // assert
            assertThat(result.getSessionId()).isEqualTo(normal.getSessionId());
            assertThat(redis.opsForZSet().size(keys.elevatedIndex(USER_ID))).isEqualTo(3L);
            assertThat(redis.opsForZSet().size(keys.baseIndex(USER_ID))).isEqualTo(3L);
            assertThat(redis.hasKey(keys.session(firstAdmin.getSessionId()))).isFalse();
            assertThat(redis.opsForZSet().score(keys.elevatedIndex(USER_ID), normal.getSessionId())).isNotNull();
            // verify
        }

        @Test
        @DisplayName("malformed and unsupported families fail closed without index mutation")
        void malformedAndUnsupportedFamiliesFailClosedWithoutIndexMutation() throws Exception {
            // arrange
            RefreshSessionLifecycleResult malformed = store.create(baseCreateParam("malformed"));
            redis.opsForValue().set(keys.session(malformed.getSessionId()), "{}");
            Double malformedScore = redis.opsForZSet().score(keys.baseIndex(USER_ID), malformed.getSessionId());
            // conditions
            // act
            RefreshSessionLifecycleException malformedFailure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.rotate(baseRotateParam(malformed.getRefreshToken())));
            // assert
            assertThat(malformedFailure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.MALFORMED_STATE);
            assertThat(redis.opsForZSet().score(keys.baseIndex(USER_ID), malformed.getSessionId()))
                    .isEqualTo(malformedScore);
            // arrange
            deleteProposalTestKeys();
            RefreshSessionLifecycleResult unsupported = store.create(baseCreateParam("unsupported"));
            ObjectNode json = (ObjectNode) jsonMapper.readTree(
                    redis.opsForValue().get(keys.session(unsupported.getSessionId())));
            json.put("recordVersion", 99);
            redis.opsForValue().set(keys.session(unsupported.getSessionId()), jsonMapper.writeValueAsString(json));
            // act
            RefreshSessionLifecycleException unsupportedFailure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.rotate(baseRotateParam(unsupported.getRefreshToken())));
            // assert
            assertThat(unsupportedFailure.getReason())
                    .isEqualTo(RefreshSessionLifecycleException.Reason.UNSUPPORTED_RECORD_VERSION);
            assertThat(redis.opsForZSet().score(keys.baseIndex(USER_ID), unsupported.getSessionId())).isNotNull();
            // verify
        }

        @Test
        @DisplayName("wrong elevated index type does not partially rotate family or base index")
        void wrongElevatedIndexTypeDoesNotPartiallyRotateFamilyOrBaseIndex() {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));
            String familyBefore = redis.opsForValue().get(keys.session(created.getSessionId()));
            Double scoreBefore = redis.opsForZSet().score(keys.baseIndex(USER_ID), created.getSessionId());
            redis.opsForValue().set(keys.elevatedIndex(USER_ID), "wrong-type");
            // conditions
            // act
            RefreshSessionLifecycleException failure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.rotate(baseRotateParam(created.getRefreshToken())));
            // assert
            assertThat(failure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.WRONG_REDIS_TYPE);
            assertThat(redis.opsForValue().get(keys.session(created.getSessionId()))).isEqualTo(familyBefore);
            assertThat(redis.opsForZSet().score(keys.baseIndex(USER_ID), created.getSessionId()))
                    .isEqualTo(scoreBefore);
            assertThat(redis.hasKey(keys.grace(created.getSessionId()))).isFalse();
            // verify
        }

        @Test
        @DisplayName("wrong grace value type rejects without changing the current family")
        void wrongGraceValueTypeRejectsWithoutChangingTheCurrentFamily() {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));
            RefreshSessionLifecycleResult rotated = store.rotate(baseRotateParam(created.getRefreshToken()));
            String familyBefore = redis.opsForValue().get(keys.session(created.getSessionId()));
            redis.delete(keys.grace(created.getSessionId()));
            redis.opsForSet().add(keys.grace(created.getSessionId()), "wrong-type");
            // conditions
            // act
            RefreshSessionLifecycleException failure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.rotate(baseRotateParam(created.getRefreshToken())));
            // assert
            assertThat(failure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.WRONG_REDIS_TYPE);
            assertThat(redis.opsForValue().get(keys.session(created.getSessionId()))).isEqualTo(familyBefore);
            assertThat(serializer.decode(familyBefore).getCurrentRefreshTokenHash())
                    .isEqualTo(rotated.getFamily().getCurrentRefreshTokenHash());
            assertThat(redis.opsForZSet().size(keys.baseIndex(USER_ID))).isEqualTo(1L);
            // verify
        }

    }

    @Nested
    @DisplayName("Grace Recovery")
    class GraceRecoveryTests {

        @Test
        @DisplayName("simultaneous presentation converge on one successor and never branch")
        void simultaneousPresentationsConvergeOnOneSuccessorAndNeverBranch() throws Exception {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));
            CyclicBarrier barrier = new CyclicBarrier(3);
            ExecutorService executor = newExecutor(2);
            Future<RefreshSessionLifecycleResult> first = executor.submit(() -> {
                barrier.await();
                return store.rotate(baseRotateParam(created.getRefreshToken()));
            });
            Future<RefreshSessionLifecycleResult> second = executor.submit(() -> {
                barrier.await();
                return store.rotate(baseRotateParam(created.getRefreshToken()));
            });
            // conditions
            // act
            barrier.await();
            RefreshSessionLifecycleResult firstResult = first.get(10, TimeUnit.SECONDS);
            RefreshSessionLifecycleResult secondResult = second.get(10, TimeUnit.SECONDS);
            // assert
            assertThat(firstResult.getRefreshToken()).isEqualTo(secondResult.getRefreshToken());
            assertThat(Set.of(firstResult.getOutcome(), secondResult.getOutcome()))
                    .containsExactlyInAnyOrder(
                            RefreshSessionLifecycleOutcome.ROTATED,
                            RefreshSessionLifecycleOutcome.GRACE_RECOVERED);
            RefreshSessionFamilyRecord family = serializer.decode(
                    redis.opsForValue().get(keys.session(created.getSessionId())));
            assertThat(family.getRotationCounter()).isEqualTo(1L);
            assertThat(redis.opsForZSet().size(keys.baseIndex(USER_ID))).isEqualTo(1L);
            ObjectNode grace = (ObjectNode) jsonMapper.readTree(
                    redis.opsForValue().get(keys.grace(created.getSessionId())));
            assertThat(grace.get("consumed").booleanValue()).isTrue();
            // verify
        }

        @Test
        @DisplayName("an additional previous token replay revokes the family")
        void anAdditionalPreviousTokenReplayRevokesTheFamily() {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));
            store.rotate(baseRotateParam(created.getRefreshToken()));
            store.rotate(baseRotateParam(created.getRefreshToken()));
            // conditions
            // act
            RefreshSessionLifecycleException failure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.rotate(baseRotateParam(created.getRefreshToken())));
            // assert
            assertThat(failure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.REPLAY_REVOKED);
            assertFamilyCompletelyAbsent(created.getSessionId());
            // verify
        }

        @Test
        @DisplayName("previous token after five seconds revokes without extending grace")
        void previousTokenAfterFiveSecondsRevokesWithoutExtendingGrace() {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));
            store.rotate(baseRotateParam(created.getRefreshToken()));
            Long originalTtl = redis.getExpire(keys.grace(created.getSessionId()), TimeUnit.MILLISECONDS);
            clock.advance(Duration.ofSeconds(5));
            // conditions
            // act
            RefreshSessionLifecycleException failure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.rotate(baseRotateParam(created.getRefreshToken())));
            // assert
            assertThat(originalTtl).isPositive().isLessThanOrEqualTo(5_100L);
            assertThat(failure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.REPLAY_REVOKED);
            assertFamilyCompletelyAbsent(created.getSessionId());
            // verify
        }

        @Test
        @DisplayName("client binding mismatch revokes the family")
        void clientBindingMismatchRevokesTheFamily() {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));
            store.rotate(baseRotateParam(created.getRefreshToken()));
            // conditions
            // act
            RefreshSessionLifecycleException failure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.rotate(RefreshSessionFamilyRotateParam.builder()
                            .presentedRefreshToken(created.getRefreshToken())
                            .roles(List.of("USER"))
                            .authVersion(7L)
                            .clientBindingHash(OTHER_CLIENT_BINDING)
                            .deviceLabel("browser")
                            .build()));
            // assert
            assertThat(failure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.REPLAY_REVOKED);
            assertFamilyCompletelyAbsent(created.getSessionId());
            // verify
        }

        @Test
        @DisplayName("redis never contains plantext successor creds")
        void RedisNeverContainsPlaintextSuccessorCredential() {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));
            // conditions
            // act
            RefreshSessionLifecycleResult rotated = store.rotate(baseRotateParam(created.getRefreshToken()));
            // assert
            String familyJson = redis.opsForValue().get(keys.session(created.getSessionId()));
            String graceJson = redis.opsForValue().get(keys.grace(created.getSessionId()));
            assertThat(familyJson).doesNotContain(rotated.getRefreshToken());
            assertThat(graceJson).doesNotContain(rotated.getRefreshToken());
            assertThat(graceJson).contains("successorEnvelope");
            // verify
        }

        @Test
        @DisplayName("decryption failure revokes the family and returns no creds")
        void decryptionFailureRevokesTheFamilyAndReturnsNoCredential() throws Exception {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));
            store.rotate(baseRotateParam(created.getRefreshToken()));
            String graceKey = keys.grace(created.getSessionId());
            Long remainingTtl = redis.getExpire(graceKey, TimeUnit.MILLISECONDS);
            ObjectNode grace = (ObjectNode) jsonMapper.readTree(redis.opsForValue().get(graceKey));
            grace.put("successorEnvelope", "v1.invalid.invalid");
            redis.opsForValue().set(
                    graceKey,
                    jsonMapper.writeValueAsString(grace),
                    Expiration.milliseconds(remainingTtl));
            // conditions
            // act
            RefreshSessionLifecycleException failure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.rotate(baseRotateParam(created.getRefreshToken())));
            // assert
            assertThat(failure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.ENVELOPE_FAILURE);
            assertFamilyCompletelyAbsent(created.getSessionId());
            // verify
        }

    }

    @Nested
    @DisplayName("inventory(String, String, Instant)")
    class InventoryTests {

        @Test
        @DisplayName("returns owned families and repairs base and elevated index drift")
        void returnsOwnedFamiliesAndRepairsIndexDrift() {
            // arrange
            RefreshSessionLifecycleResult normal = store.create(baseCreateParam("normal"));
            clock.advance(Duration.ofSeconds(1));
            RefreshSessionLifecycleResult current = store.create(adminCreateParam("current-admin"));
            redis.opsForZSet().remove(keys.baseIndex(USER_ID), normal.getSessionId());
            redis.opsForZSet().remove(keys.baseIndex(USER_ID), current.getSessionId());
            redis.opsForZSet().remove(keys.elevatedIndex(USER_ID), current.getSessionId());
            redis.opsForZSet().add(
                    keys.elevatedIndex(USER_ID),
                    normal.getSessionId(),
                    normal.getFamily().getLastUsedAt().toEpochMilli());
            // conditions
            // act
            List<RefreshSessionFamilySnapshot> result = store.inventory(
                    USER_ID,
                    current.getSessionId(),
                    clock.instant());
            // assert
            assertThat(result)
                    .extracting(snapshot -> snapshot.getSessionId().toString())
                    .containsExactlyInAnyOrder(normal.getSessionId(), current.getSessionId());
            assertThat(redis.opsForZSet().score(keys.baseIndex(USER_ID), current.getSessionId()))
                    .isEqualTo((double) current.getFamily().getLastUsedAt().toEpochMilli());
            assertThat(redis.opsForZSet().score(keys.baseIndex(USER_ID), normal.getSessionId()))
                    .isEqualTo((double) normal.getFamily().getLastUsedAt().toEpochMilli());
            assertThat(redis.opsForZSet().score(keys.elevatedIndex(USER_ID), current.getSessionId()))
                    .isEqualTo((double) current.getFamily().getLastUsedAt().toEpochMilli());
            assertThat(redis.opsForZSet().score(keys.elevatedIndex(USER_ID), normal.getSessionId())).isNull();
            // verify
        }

        @Test
        @DisplayName("cleans missing indexed family and unusable grace state")
        void cleansMissingIndexedFamilyAndGraceState() {
            // arrange
            RefreshSessionLifecycleResult current = store.create(baseCreateParam("current"));
            RefreshSessionLifecycleResult missing = store.create(baseCreateParam("missing"));
            redis.opsForValue().set(keys.grace(missing.getSessionId()), "unusable-grace");
            redis.delete(keys.session(missing.getSessionId()));
            // conditions
            // act
            List<RefreshSessionFamilySnapshot> result = store.inventory(
                    USER_ID,
                    current.getSessionId(),
                    clock.instant());
            // assert
            assertThat(result)
                    .extracting(snapshot -> snapshot.getSessionId().toString())
                    .containsExactly(current.getSessionId());
            assertFamilyCompletelyAbsent(missing.getSessionId());
            // verify
        }

        @Test
        @DisplayName("omits and compare deletes expired non current family")
        void omitsAndCompareDeletesExpiredNonCurrentFamily() {
            // arrange
            RefreshSessionLifecycleResult expired = store.create(baseCreateParam("expired"));
            clock.advance(Duration.ofDays(6));
            RefreshSessionLifecycleResult current = store.create(baseCreateParam("current"));
            clock.advance(Duration.ofDays(1));
            // conditions
            // act
            List<RefreshSessionFamilySnapshot> result = store.inventory(
                    USER_ID,
                    current.getSessionId(),
                    clock.instant());
            // assert
            assertThat(result)
                    .extracting(snapshot -> snapshot.getSessionId().toString())
                    .containsExactly(current.getSessionId());
            assertFamilyCompletelyAbsent(expired.getSessionId());
            // verify
        }

        @Test
        @DisplayName("missing or expired current family fails authentication state")
        void missingOrExpiredCurrentFamilyFailsAuthenticationState() {
            // arrange
            RefreshSessionLifecycleResult missing = store.create(baseCreateParam("missing-current"));
            redis.delete(keys.session(missing.getSessionId()));
            // conditions
            // act
            RefreshSessionLifecycleException missingFailure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.inventory(USER_ID, missing.getSessionId(), clock.instant()));
            // assert
            assertThat(missingFailure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.MISSING_FAMILY);
            // arrange
            RefreshSessionLifecycleResult expired = store.create(baseCreateParam("expired-current"));
            clock.advance(Duration.ofDays(7));
            // conditions
            // act
            RefreshSessionLifecycleException expiredFailure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.inventory(USER_ID, expired.getSessionId(), clock.instant()));
            // assert
            assertThat(expiredFailure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.EXPIRED_FAMILY);
            assertFamilyCompletelyAbsent(expired.getSessionId());
            // verify
        }

        @Test
        @DisplayName("malformed or cross owner state fails without partial inventory or foreign deletion")
        void malformedOrCrossOwnerStateFailsWithoutPartialInventoryOrForeignDeletion() {
            // arrange
            RefreshSessionLifecycleResult malformedCurrent = store.create(baseCreateParam("current"));
            RefreshSessionLifecycleResult malformed = store.create(baseCreateParam("malformed"));
            redis.opsForValue().set(keys.session(malformed.getSessionId()), "{}");
            // conditions
            // act
            RefreshSessionLifecycleException malformedFailure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.inventory(USER_ID, malformedCurrent.getSessionId(), clock.instant()));
            // assert
            assertThat(malformedFailure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.MALFORMED_STATE);
            assertThat(redis.hasKey(keys.session(malformedCurrent.getSessionId()))).isTrue();
            assertThat(redis.hasKey(keys.session(malformed.getSessionId()))).isTrue();
            // arrange
            deleteProposalTestKeys();
            RefreshSessionLifecycleResult foreignCurrent = store.create(baseCreateParam("current"));
            RefreshSessionLifecycleResult foreign = store.create(foreignCreateParam("foreign"));
            redis.opsForZSet().add(
                    keys.baseIndex(USER_ID),
                    foreign.getSessionId(),
                    foreign.getFamily().getLastUsedAt().toEpochMilli());
            String currentSessionId = foreignCurrent.getSessionId();
            // conditions
            // act
            RefreshSessionLifecycleException foreignFailure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.inventory(USER_ID, currentSessionId, clock.instant()));
            // assert
            assertThat(foreignFailure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.MALFORMED_STATE);
            assertThat(redis.hasKey(keys.session(foreign.getSessionId()))).isTrue();
            assertThat(redis.opsForZSet().score(keys.baseIndex(FOREIGN_USER_ID), foreign.getSessionId())).isNotNull();
            // verify
        }

        @Test
        @DisplayName("unsupported record fails complete inventory without deleting family")
        void unsupportedRecordFailsCompleteInventoryWithoutDeletingFamily() throws Exception {
            // arrange
            RefreshSessionLifecycleResult current = store.create(baseCreateParam("current"));
            RefreshSessionLifecycleResult unsupported = store.create(baseCreateParam("unsupported"));
            ObjectNode json = (ObjectNode) jsonMapper.readTree(
                    redis.opsForValue().get(keys.session(unsupported.getSessionId())));
            json.put("recordVersion", 99);
            redis.opsForValue().set(keys.session(unsupported.getSessionId()), jsonMapper.writeValueAsString(json));
            // conditions
            // act
            RefreshSessionLifecycleException failure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.inventory(USER_ID, current.getSessionId(), clock.instant()));
            // assert
            assertThat(failure.getReason())
                    .isEqualTo(RefreshSessionLifecycleException.Reason.UNSUPPORTED_RECORD_VERSION);
            assertThat(redis.hasKey(keys.session(unsupported.getSessionId()))).isTrue();
            assertThat(redis.hasKey(keys.session(current.getSessionId()))).isTrue();
            // verify
        }

        @Test
        @DisplayName("wrong index type and over capacity state fail before inventory work")
        void wrongIndexTypeAndOverCapacityStateFailBeforeInventoryWork() {
            // arrange
            RefreshSessionLifecycleResult wrongTypeCurrent = store.create(baseCreateParam("current"));
            redis.delete(keys.baseIndex(USER_ID));
            redis.opsForValue().set(keys.baseIndex(USER_ID), "wrong-type");
            // conditons
            // act
            RefreshSessionLifecycleException typeFailure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.inventory(USER_ID, wrongTypeCurrent.getSessionId(), clock.instant()));
            // assert
            assertThat(typeFailure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.WRONG_REDIS_TYPE);
            // arrange
            deleteProposalTestKeys();
            RefreshSessionLifecycleResult capacityCurrent = store.create(baseCreateParam("current"));
            for (int i = 0; i < 10; i++) {
                redis.opsForZSet().add(
                        keys.baseIndex(USER_ID),
                        String.format("00000000-0000-0000-0000-%012d", i),
                        i);
            }
            String currentSessionId = capacityCurrent.getSessionId();
            // condtions
            // act
            RefreshSessionLifecycleException capacityFailure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.inventory(USER_ID, currentSessionId, clock.instant()));
            // assert
            assertThat(capacityFailure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.MALFORMED_STATE);
            // verify
        }

        @Test
        @DisplayName("inventory racing rotation returns one valid linearized snapshot")
        void inventoryRacingRotationReturnsOneValidLinearizedSnapshot() throws Exception {
            // arrange
            RefreshSessionLifecycleResult current = store.create(baseCreateParam("current"));
            CyclicBarrier barrier = new CyclicBarrier(3);
            ExecutorService executor = newExecutor(2);
            Future<List<RefreshSessionFamilySnapshot>> inventory = executor.submit(() -> {
                barrier.await();
                return store.inventory(USER_ID, current.getSessionId(), clock.instant());
            });
            Future<RefreshSessionLifecycleResult> rotation = executor.submit(() -> {
                barrier.await();
                return store.rotate(baseRotateParam(current.getRefreshToken()));
            });
            // conditions
            // act
            barrier.await();
            List<RefreshSessionFamilySnapshot> inventoryResult = inventory.get(10, TimeUnit.SECONDS);
            RefreshSessionLifecycleResult rotationResult = rotation.get(10, TimeUnit.SECONDS);
            // assert
            assertThat(inventoryResult)
                    .extracting(snapshot -> snapshot.getSessionId().toString())
                    .containsExactly(current.getSessionId());
            assertThat(rotationResult.getSessionId()).isEqualTo(current.getSessionId());
            assertThat(redis.hasKey(keys.session(current.getSessionId()))).isTrue();
            // verify
        }

        @Test
        @DisplayName("inventory racing capacity eviction remains bounded and consistent")
        void inventoryRacingCapacityEvictionRemainsBoundedAndConsistent() throws Exception {
            // arrange
            for (int i = 0; i < 9; i++) {
                store.create(baseCreateParam("existing-" + i));
                clock.advance(Duration.ofSeconds(1));
            }
            RefreshSessionLifecycleResult current = store.create(baseCreateParam("current"));
            CyclicBarrier barrier = new CyclicBarrier(3);
            ExecutorService executor = newExecutor(2);
            Future<List<RefreshSessionFamilySnapshot>> inventory = executor.submit(() -> {
                barrier.await();
                return store.inventory(USER_ID, current.getSessionId(), clock.instant());
            });
            Future<RefreshSessionLifecycleResult> creation = executor.submit(() -> {
                barrier.await();
                return store.create(baseCreateParam("capacity"));
            });
            // conditions
            // act
            barrier.await();
            List<RefreshSessionFamilySnapshot> result = inventory.get(10, TimeUnit.SECONDS);
            creation.get(10, TimeUnit.SECONDS);
            // assert
            assertThat(result).hasSize(10);
            assertThat(result)
                    .extracting(snapshot -> snapshot.getSessionId().toString())
                    .contains(current.getSessionId());
            assertThat(redis.opsForZSet().size(keys.baseIndex(USER_ID))).isEqualTo(10L);
            // verify
        }

        @Test
        @DisplayName("stale cleanup racing rotation never deletes a changed successor")
        void staleCleanupRacingRotationNeverDeletesChangedSuccessor() throws Exception {
            // arrange
            RefreshSessionLifecycleResult stale = store.create(baseCreateParam("stale"));
            clock.advance(Duration.ofDays(6));
            RefreshSessionLifecycleResult current = store.create(baseCreateParam("current"));
            clock.advance(Duration.ofDays(1));
            CyclicBarrier barrier = new CyclicBarrier(3);
            ExecutorService executor = newExecutor(2);
            Future<List<RefreshSessionFamilySnapshot>> inventory = executor.submit(() -> {
                barrier.await();
                return store.inventory(USER_ID, current.getSessionId(), clock.instant());
            });
            Future<?> rotation = rotatingTask(executor, barrier, stale);
            // conditions
            // act
            barrier.await();
            List<RefreshSessionFamilySnapshot> result = inventory.get(10, TimeUnit.SECONDS);
            rotation.get(10, TimeUnit.SECONDS);
            // assert
            assertThat(result)
                    .extracting(snapshot -> snapshot.getSessionId().toString())
                    .containsExactly(current.getSessionId());
            assertFamilyCompletelyAbsent(stale.getSessionId());
            // verify
        }

    }

    @Nested
    @DisplayName("Production read and presented-token revocation")
    class ProductionCutoverTests {

        @Test
        @DisplayName("strictly reads the authoritative v2 family")
        void strictlyReadsAuthoritativeV2Family() {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));

            // act
            RefreshSessionFamilyRecord result = store.requireFamily(created.getSessionId());

            // assert
            assertThat(result.getRecordVersion()).isEqualTo(RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION);
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getRotationCounter()).isZero();
            // verify
        }

        @Test
        @DisplayName("current presented token revokes family and all derived state")
        void currentPresentedTokenRevokesFamilyAndDerivedState() {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));

            // act
            boolean revoked = store.revokePresented(created.getRefreshToken());

            // assert
            assertThat(revoked).isTrue();
            assertFamilyCompletelyAbsent(created.getSessionId());
            // verify
        }

        @Test
        @DisplayName("immediately previous presented token revokes the already-rotated family")
        void previousPresentedTokenRevokesRotatedFamily() {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));
            store.rotate(baseRotateParam(created.getRefreshToken()));

            // act
            boolean revoked = store.revokePresented(created.getRefreshToken());

            // assert
            assertThat(revoked).isTrue();
            assertFamilyCompletelyAbsent(created.getSessionId());
            // verify
        }

        @Test
        @DisplayName("unrelated token cannot revoke a family merely by knowing sid")
        void unrelatedTokenCannotRevokeFamily() {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));

            // act
            boolean revoked = store.revokePresented(created.getSessionId() + ".unrelated");

            // assert
            assertThat(revoked).isFalse();
            assertThat(redis.hasKey(keys.session(created.getSessionId()))).isTrue();
            assertThat(redis.opsForZSet().score(keys.baseIndex(USER_ID), created.getSessionId())).isNotNull();
            // verify
        }
    }

    @Nested
    @DisplayName("Revoke")
    class RevokeTests {

        @Test
        @DisplayName("revoke one deletes family grace and both index membership")
        void revokeOneDeletesFamilyGraceAndBothIndexMemberships() {
            // arrange
            RefreshSessionLifecycleResult created = store.create(adminCreateParam("admin"));
            store.rotate(adminRotateParam(created.getRefreshToken()));
            // conditions
            // act
            store.revokeOne(USER_ID, created.getSessionId());
            // assert
            assertFamilyCompletelyAbsent(created.getSessionId());
            // verify
        }

        @Test
        @DisplayName("revoke one makes missing and foreign targets no op without deleting foreign state")
        void revokeOneMakesMissingAndForeignTargetsNoOp() {
            // arrange
            String missingSessionId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
            RefreshSessionLifecycleResult foreign = store.create(foreignCreateParam("foreign"));
            // conditions
            // act
            store.revokeOne(USER_ID, missingSessionId);
            store.revokeOne(USER_ID, foreign.getSessionId());
            // assert
            assertThat(redis.hasKey(keys.session(foreign.getSessionId()))).isTrue();
            assertThat(redis.opsForZSet().score(keys.baseIndex(FOREIGN_USER_ID), foreign.getSessionId())).isNotNull();
            // verify
        }

        @Test
        @DisplayName("revoke others retains only the selected owned family")
        void revokeOthersRetainsOnlyTheSelectedOwnedFamily() {
            // arrange
            RefreshSessionLifecycleResult first = store.create(baseCreateParam("first"));
            RefreshSessionLifecycleResult retained = store.create(baseCreateParam("retained"));
            RefreshSessionLifecycleResult third = store.create(adminCreateParam("third"));
            RefreshSessionLifecycleResult missing = store.create(baseCreateParam("missing"));
            redis.opsForValue().set(keys.grace(missing.getSessionId()), "unusable-grace");
            redis.delete(keys.session(missing.getSessionId()));
            // conditions
            // act
            store.revokeOthers(USER_ID, retained.getSessionId());
            // assert
            assertFamilyCompletelyAbsent(first.getSessionId());
            assertFamilyCompletelyAbsent(third.getSessionId());
            assertFamilyCompletelyAbsent(missing.getSessionId());
            assertThat(redis.hasKey(keys.session(retained.getSessionId()))).isTrue();
            assertThat(redis.opsForZSet().size(keys.baseIndex(USER_ID))).isEqualTo(1L);
            // verify
        }

        @Test
        @DisplayName("revoke others repairs unindexed current and preserves its grace")
        void revokeOthersRepairsUnindexedCurrentAndPreservesGrace() {
            // arrange
            RefreshSessionLifecycleResult other = store.create(baseCreateParam("other"));
            RefreshSessionLifecycleResult current = store.create(adminCreateParam("current"));
            RefreshSessionLifecycleResult rotated = store.rotate(adminRotateParam(current.getRefreshToken()));
            String currentFamilyBefore = redis.opsForValue().get(keys.session(current.getSessionId()));
            String currentGraceBefore = redis.opsForValue().get(keys.grace(current.getSessionId()));
            redis.opsForZSet().remove(keys.baseIndex(USER_ID), current.getSessionId());
            redis.opsForZSet().remove(keys.elevatedIndex(USER_ID), current.getSessionId());

            // act
            store.revokeOthers(USER_ID, current.getSessionId());

            // assert
            assertFamilyCompletelyAbsent(other.getSessionId());
            assertThat(redis.opsForValue().get(keys.session(current.getSessionId()))).isEqualTo(currentFamilyBefore);
            assertThat(redis.opsForValue().get(keys.grace(current.getSessionId()))).isEqualTo(currentGraceBefore);
            assertThat(redis.opsForZSet().score(keys.baseIndex(USER_ID), current.getSessionId()))
                    .isEqualTo((double) rotated.getFamily().getLastUsedAt().toEpochMilli());
            assertThat(redis.opsForZSet().score(keys.elevatedIndex(USER_ID), current.getSessionId())).isNotNull();
        }

        @Test
        @DisplayName("revoke others fails before partial revocation on cross owner corruption")
        void revokeOthersFailsBeforePartialRevocationOnCrossOwnerCorruption() {
            // arrange
            RefreshSessionLifecycleResult other = store.create(baseCreateParam("other"));
            RefreshSessionLifecycleResult current = store.create(baseCreateParam("current"));
            RefreshSessionLifecycleResult foreign = store.create(foreignCreateParam("foreign"));
            redis.opsForZSet().add(
                    keys.baseIndex(USER_ID),
                    foreign.getSessionId(),
                    foreign.getFamily().getLastUsedAt().toEpochMilli());
            // conditions
            // act
            RefreshSessionLifecycleException failure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.revokeOthers(USER_ID, current.getSessionId()));
            // assert
            assertThat(failure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.MALFORMED_STATE);
            assertThat(redis.hasKey(keys.session(other.getSessionId()))).isTrue();
            assertThat(redis.hasKey(keys.session(current.getSessionId()))).isTrue();
            assertThat(redis.hasKey(keys.session(foreign.getSessionId()))).isTrue();
            // verify
        }

        @Test
        @DisplayName("revoke all deletes every indexed family and both indexes")
        void revokeAllDeletesEveryIndexedFamilyAndBothIndexes() {
            // arrange
            RefreshSessionLifecycleResult first = store.create(baseCreateParam("first"));
            RefreshSessionLifecycleResult second = store.create(adminCreateParam("second"));
            store.rotate(adminRotateParam(second.getRefreshToken()));
            // conditions
            // act
            store.revokeAll(USER_ID);
            // assert
            assertFamilyCompletelyAbsent(first.getSessionId());
            assertFamilyCompletelyAbsent(second.getSessionId());
            assertThat(redis.hasKey(keys.baseIndex(USER_ID))).isFalse();
            assertThat(redis.hasKey(keys.elevatedIndex(USER_ID))).isFalse();
            // verify
        }

        @Test
        @DisplayName("public revoke all includes explicit current missing both memberships")
        void publicRevokeAllIncludesExplicitCurrentMissingBothMemberships() {
            // arrange
            RefreshSessionLifecycleResult other = store.create(baseCreateParam("other"));
            RefreshSessionLifecycleResult current = store.create(adminCreateParam("current"));
            redis.opsForZSet().remove(keys.baseIndex(USER_ID), current.getSessionId());
            redis.opsForZSet().remove(keys.elevatedIndex(USER_ID), current.getSessionId());
            // conditions
            // act
            store.revokeAll(USER_ID, current.getSessionId());
            // assert
            assertFamilyCompletelyAbsent(other.getSessionId());
            assertFamilyCompletelyAbsent(current.getSessionId());
            assertThat(redis.hasKey(keys.baseIndex(USER_ID))).isFalse();
            assertThat(redis.hasKey(keys.elevatedIndex(USER_ID))).isFalse();
            // verify
        }

        @Test
        @DisplayName("revoke all fails closed on corrupt foreign membership without deleting any family")
        void revokeAllFailsClosedOnCorruptForeignMembershipWithoutDeletingAnyFamily() {
            // arrange
            RefreshSessionLifecycleResult owned = store.create(baseCreateParam("owned"));
            RefreshSessionLifecycleResult foreign = store.create(foreignCreateParam("foreign"));
            redis.opsForZSet().add(
                    keys.baseIndex(USER_ID),
                    foreign.getSessionId(),
                    foreign.getFamily().getLastUsedAt().toEpochMilli());
            // conditions
            // act
            RefreshSessionLifecycleException failure = catchThrowableOfType(
                    RefreshSessionLifecycleException.class,
                    () -> store.revokeAll(USER_ID));
            // assert
            assertThat(failure.getReason()).isEqualTo(RefreshSessionLifecycleException.Reason.MALFORMED_STATE);
            assertThat(redis.hasKey(keys.session(owned.getSessionId()))).isTrue();
            assertThat(redis.hasKey(keys.session(foreign.getSessionId()))).isTrue();
            assertThat(redis.opsForZSet().score(keys.baseIndex(FOREIGN_USER_ID), foreign.getSessionId()))
                    .isNotNull();
            assertThat(redis.opsForZSet().score(keys.baseIndex(USER_ID), foreign.getSessionId())).isNotNull();
            // verify
        }

        @Test
        @DisplayName("rotation racing revocation cannot resurrect the family")
        void rotationRacingRevocationCannotResurrectTheFamily() throws Exception {
            // arrange
            RefreshSessionLifecycleResult created = store.create(baseCreateParam("browser"));
            CyclicBarrier barrier = new CyclicBarrier(3);
            ExecutorService executor = newExecutor(2);
            Future<?> rotation = executor.submit(() -> {
                barrier.await();
                try {
                    store.rotate(baseRotateParam(created.getRefreshToken()));
                } catch (RefreshSessionLifecycleException ignored) {
                    // Revocation may linearize first.
                }
                return null;
            });
            Future<?> revocation = executor.submit(() -> {
                barrier.await();
                store.revokeOne(USER_ID, created.getSessionId());
                return null;
            });
            // conditions
            // act
            barrier.await();
            rotation.get(10, TimeUnit.SECONDS);
            revocation.get(10, TimeUnit.SECONDS);
            // assert
            assertFamilyCompletelyAbsent(created.getSessionId());
            // verify
        }

        @Test
        @DisplayName("revoke others racing current and non current rotation preserves only current")
        void revokeOthersRacingRotationsPreservesOnlyCurrent() throws Exception {
            // arrange
            RefreshSessionLifecycleResult other = store.create(baseCreateParam("other"));
            RefreshSessionLifecycleResult current = store.create(baseCreateParam("current"));
            CyclicBarrier barrier = new CyclicBarrier(4);
            ExecutorService executor = newExecutor(3);
            Future<?> otherRotation = rotatingTask(executor, barrier, other);
            Future<?> currentRotation = rotatingTask(executor, barrier, current);
            Future<?> revocation = executor.submit(() -> {
                barrier.await();
                store.revokeOthers(USER_ID, current.getSessionId());
                return null;
            });
            // conditions
            // act
            barrier.await();
            otherRotation.get(10, TimeUnit.SECONDS);
            currentRotation.get(10, TimeUnit.SECONDS);
            revocation.get(10, TimeUnit.SECONDS);
            // assert
            assertFamilyCompletelyAbsent(other.getSessionId());
            assertThat(redis.hasKey(keys.session(current.getSessionId()))).isTrue();
            assertThat(redis.opsForZSet().score(keys.baseIndex(USER_ID), current.getSessionId())).isNotNull();
            // verify
        }

        @Test
        @DisplayName("revoke all racing rotation cannot resurrect any in scope family")
        void revokeAllRacingRotationCannotResurrectAnyFamily() throws Exception {
            // arrange
            RefreshSessionLifecycleResult current = store.create(baseCreateParam("current"));
            CyclicBarrier barrier = new CyclicBarrier(3);
            ExecutorService executor = newExecutor(2);
            Future<?> rotation = rotatingTask(executor, barrier, current);
            Future<?> revocation = executor.submit(() -> {
                barrier.await();
                store.revokeAll(USER_ID, current.getSessionId());
                return null;
            });
            // conditions
            // act
            barrier.await();
            rotation.get(10, TimeUnit.SECONDS);
            revocation.get(10, TimeUnit.SECONDS);
            // assert
            assertFamilyCompletelyAbsent(current.getSessionId());
            assertThat(redis.hasKey(keys.baseIndex(USER_ID))).isFalse();
            assertThat(redis.hasKey(keys.elevatedIndex(USER_ID))).isFalse();
            // verify
        }

    }

    private RefreshSessionFamilyCreateParam baseCreateParam(String deviceLabel) {
        return RefreshSessionFamilyCreateParam.builder()
                .userId(USER_ID)
                .instanceId(INSTANCE_ID)
                .roles(List.of("USER"))
                .authVersion(7L)
                .deviceLabel(deviceLabel)
                .clientBindingHash(CLIENT_BINDING)
                .build();
    }

    private RefreshSessionFamilyCreateParam adminCreateParam(String deviceLabel) {
        return RefreshSessionFamilyCreateParam.builder()
                .userId(USER_ID)
                .instanceId(INSTANCE_ID)
                .roles(List.of("INSTANCE_ADMIN", "USER"))
                .authVersion(7L)
                .adminSessionVersion(3L)
                .deviceLabel(deviceLabel)
                .clientBindingHash(CLIENT_BINDING)
                .build();
    }

    private RefreshSessionFamilyCreateParam foreignCreateParam(String deviceLabel) {
        return RefreshSessionFamilyCreateParam.builder()
                .userId(FOREIGN_USER_ID)
                .instanceId(INSTANCE_ID)
                .roles(List.of("USER"))
                .authVersion(4L)
                .deviceLabel(deviceLabel)
                .clientBindingHash(CLIENT_BINDING)
                .build();
    }

    private RefreshSessionFamilyRotateParam baseRotateParam(String refreshToken) {
        return RefreshSessionFamilyRotateParam.builder()
                .presentedRefreshToken(refreshToken)
                .roles(List.of("USER"))
                .authVersion(7L)
                .deviceLabel("browser")
                .clientBindingHash(CLIENT_BINDING)
                .build();
    }

    private RefreshSessionFamilyRotateParam adminRotateParam(String refreshToken) {
        return RefreshSessionFamilyRotateParam.builder()
                .presentedRefreshToken(refreshToken)
                .roles(List.of("INSTANCE_ADMIN", "USER"))
                .authVersion(7L)
                .adminSessionVersion(3L)
                .deviceLabel("admin")
                .clientBindingHash(CLIENT_BINDING)
                .build();
    }

    private ExecutorService newExecutor(int threads) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        executors.add(executor);
        return executor;
    }

    private Future<?> rotatingTask(
            ExecutorService executor,
            CyclicBarrier barrier,
            RefreshSessionLifecycleResult family) {
        return executor.submit(() -> {
            barrier.await();
            try {
                store.rotate(baseRotateParam(family.getRefreshToken()));
            } catch (RefreshSessionLifecycleException ignored) {
                // A concurrent revocation may linearize first.
            }
            return null;
        });
    }

    private void assertFamilyCompletelyAbsent(String sessionId) {
        assertThat(redis.hasKey(keys.session(sessionId))).isFalse();
        assertThat(redis.hasKey(keys.grace(sessionId))).isFalse();
        assertThat(redis.opsForZSet().score(keys.baseIndex(USER_ID), sessionId)).isNull();
        assertThat(redis.opsForZSet().score(keys.elevatedIndex(USER_ID), sessionId)).isNull();
    }

    private void deleteProposalTestKeys() {
        Set<String> existing = redis.keys(KEY_PREFIX + "*");
        if (existing != null && !existing.isEmpty()) {
            redis.delete(existing);
        }
    }

}
