package com.syncturtle.common.cache.unit.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.syncturtle.common.cache.payload.CachedResponsePayload;
import com.syncturtle.common.cache.properties.ResponseCacheProperties;
import com.syncturtle.common.cache.response.ResponseCacheAspect;
import com.syncturtle.common.cache.response.ResponseCacheKeyBuilder;
import com.syncturtle.common.cache.response.annotation.CacheResponse;
import com.syncturtle.common.cache.response.annotation.InvalidateCache;
import com.syncturtle.common.web.context.RequestUserContext;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("ResponseCacheAspect")
class ResponseCacheAspectTest {

    private static final String KEY_PREFIX = "st:local:rc:";
    private static final String SERVICE_NAME = "instance-service";

    @Mock
    StringRedisTemplate redis;
    @Mock
    ValueOperations<String, String> valueOps;
    @Mock
    ResponseCacheProperties props;
    @Mock
    RequestUserContext userContext;
    @Mock
    ProceedingJoinPoint pjp;
    @Mock
    MethodSignature methodSignature;
    @Captor
    ArgumentCaptor<String> keyCaptor;
    @Captor
    ArgumentCaptor<String> jsonCaptor;

    private JsonMapper objectMapper;
    private ResponseCacheAspect aspect;

    @BeforeEach
    void setup() {
        objectMapper = new JsonMapper();

        aspect = new ResponseCacheAspect(redis, objectMapper, props, new ResponseCacheKeyBuilder(), userContext,
                SERVICE_NAME);
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("aroundCache(ProceedingJoinPoint, CacheResponse)")
    class AroundCacheTests {

        @Test
        @DisplayName("proceeds without redis when cache is disabled")
        void proceedsWithoutRedisWhenCacheIsDisabled() throws Throwable {
            // arrange
            CacheResponse annotation = cacheAnnotation("cacheable");
            ResponseEntity<DemoBody> expected = ResponseEntity.ok(new DemoBody("fresh"));
            // conditions
            when(props.isEnabled()).thenReturn(false);
            when(pjp.proceed()).thenReturn(expected);
            // act
            Object actual = aspect.aroundCache(pjp, annotation);
            // assert
            assertThat(actual).isSameAs(expected);
            // verify
            verify(pjp).proceed();
            verifyNoInteractions(redis, userContext);
        }

        @Test
        @DisplayName("proceeds without redis when request is missing")
        void proceedsWithoutRedisWhenRequestIsMissing() throws Throwable {
            // arrange
            CacheResponse annotation = cacheAnnotation("cacheable");
            ResponseEntity<DemoBody> expected = ResponseEntity.ok(new DemoBody("fresh"));
            // conditions
            when(props.isEnabled()).thenReturn(true);
            when(pjp.proceed()).thenReturn(expected);
            // act
            Object actual = aspect.aroundCache(pjp, annotation);
            // assert
            assertThat(actual).isSameAs(expected);
            // verify
            verify(pjp).proceed();
            verifyNoInteractions(redis, userContext);
        }

        @Test
        @DisplayName("proceeds without redis for unsafe HTTP methods")
        void proceedsWithoutRedisForUnsafeHttpMethods() throws Throwable {
            // arrange
            bindRequest("POST", "/api/instances");
            CacheResponse annotation = cacheAnnotation("cacheable");
            ResponseEntity<DemoBody> expected = ResponseEntity.ok(new DemoBody("fresh"));
            // conditions
            when(props.isEnabled()).thenReturn(true);
            when(pjp.proceed()).thenReturn(expected);
            // act
            Object actual = aspect.aroundCache(pjp, annotation);
            // assert
            assertThat(actual).isSameAs(expected);
            // verify
            verify(pjp).proceed();
            verifyNoInteractions(redis, userContext);
        }

        @Test
        @DisplayName("does not cache per-user responses when user id is missing")
        void doesNotCachePerUserResponsesWhenUserIdIsMissing() throws Throwable {
            // arrange
            bindRequest("GET", "/api/instances/admins/me");
            CacheResponse annotation = cacheAnnotation("perUserCacheable");
            ResponseEntity<DemoBody> expected = ResponseEntity.ok(new DemoBody("fresh"));
            // conditions
            when(props.isEnabled()).thenReturn(true);
            when(userContext.getUserId()).thenReturn(null);
            when(pjp.proceed()).thenReturn(expected);
            // act
            Object actual = aspect.aroundCache(pjp, annotation);
            // assert
            assertThat(actual).isSameAs(expected);
            // verify
            verify(pjp).proceed();
            verifyNoInteractions(redis);
        }

        @Test
        @DisplayName("caches successful ResponseEntity on cache miss")
        void cachesSuccessfulResponseEntityOnCacheMiss() throws Throwable {
            // arrange
            MockHttpServletRequest request = bindRequest("GET", "/api/users");
            request.addParameter("page", "1");
            request.addHeader("Accept-Language", "en-US");
            CacheResponse annotation = cacheAnnotation("cacheableWithVaryHeader");
            ResponseEntity<DemoBody> expected = ResponseEntity.ok(new DemoBody("fresh"));
            // conditions
            enableCacheWriteDefaults();
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0, String.class);

                if (key.endsWith(":ver")) {
                    return "7";
                }

                if (key.contains(":data:g7")) {
                    return null;
                }

                throw new AssertionError("Unexpected redis get key: " + key);
            });
            when(pjp.proceed()).thenReturn(expected);
            // act
            Object actual = aspect.aroundCache(pjp, annotation);
            // assert
            assertThat(actual).isSameAs(expected);
            // verify
            verify(pjp).proceed();
            verify(valueOps).set(keyCaptor.capture(), jsonCaptor.capture(), eq(Duration.ofSeconds(60)));

            assertThat(keyCaptor.getValue()).startsWith("st:local:rc:instance-service:users:data:g7:").contains(":h:");

            CachedResponsePayload cached = objectMapper.readValue(jsonCaptor.getValue(), CachedResponsePayload.class);

            assertThat(cached.getStatus()).isEqualTo(200);
            assertThat(cached.getBodyType()).isEqualTo(DemoBody.class.getName());
            assertThat(cached.getBodyJson()).contains("fresh");
        }

        @Test
        @DisplayName("returns cached ResponseEntity on cache hit without proceeding")
        void returnsCachedResponseEntityOnCacheHitWithoutProceeding() throws Throwable {
            // arrange
            bindRequest("GET", "/api/users");
            CacheResponse annotation = cacheAnnotation("cacheable");
            CachedResponsePayload payload = new CachedResponsePayload();
            payload.setStatus(200);
            payload.setBodyType(DemoBody.class.getName());
            payload.setBodyJson(objectMapper.writeValueAsString(new DemoBody("cached")));
            String cachedJson = objectMapper.writeValueAsString(payload);
            // conditions
            enableCacheWriteDefaults();
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0, String.class);

                if (key.endsWith(":ver")) {
                    return "9";
                }

                if (key.contains(":data:g9")) {
                    return cachedJson;
                }

                throw new AssertionError("Unexpected redis get key: " + key);
            });
            when(pjp.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method("cacheable"));
            // act
            Object actual = aspect.aroundCache(pjp, annotation);
            // assert
            assertThat(actual).isInstanceOf(ResponseEntity.class);
            ResponseEntity<?> response = (ResponseEntity<?>) actual;

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(new DemoBody("cached"));
            // verify
            verify(pjp, never()).proceed();
            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("deletes unsafe cached payload and proceeds")
        void deletesUnsafeCachedPayloadAndProceeds() throws Throwable {
            // arrange
            bindRequest("GET", "/api/users");
            CacheResponse annotation = cacheAnnotation("cacheable");
            ResponseEntity<DemoBody> expected = ResponseEntity.ok(new DemoBody("fresh"));

            CachedResponsePayload payload = new CachedResponsePayload();
            payload.setStatus(200);
            payload.setBodyType("java.lang.Runtime");
            payload.setBodyJson("{}");
            String cachedJson = objectMapper.writeValueAsString(payload);

            enableCacheWriteDefaults();
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0, String.class);

                if (key.endsWith(":ver")) {
                    return "9";
                }

                if (key.contains(":data:g9")) {
                    return cachedJson;
                }

                throw new AssertionError("Unexpected redis get key: " + key);
            });
            when(pjp.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method("cacheable"));
            when(pjp.proceed()).thenReturn(expected);
            // act
            Object actual = aspect.aroundCache(pjp, annotation);
            // assert
            assertThat(actual).isSameAs(expected);
            // verify
            verify(redis).delete(argThat((String key) -> key != null && key.contains(":data:g9")));
            verify(pjp).proceed();
        }

        @Test
        @DisplayName("does not bump generations when cache is disabled")
        void doesNotBumpGenerationsWhenCacheIsDisabled() throws Throwable {
            // arrange
            // conditions
            when(props.isEnabled()).thenReturn(false);
            when(pjp.proceed()).thenReturn("ok");
            // act
            Object actual = aspect.aroundEvict(pjp);
            // assert
            assertThat(actual).isEqualTo("ok");
            // verify
            verify(pjp).proceed();
            verifyNoInteractions(redis);
        }

    }

    @Nested
    @DisplayName("aroundEvict(ProceedingJoinPoint)")
    class AroundEvictTests {

        @Test
        @DisplayName("bumps before invocation groups before proceed and after invocation groups after proceed")
        void bumpsBeforeAndAfterGroupsInOrder() throws Throwable {
            // arrange
            enableCacheEvictDefaults();

            when(redis.opsForValue()).thenReturn(valueOps);
            when(pjp.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method("evictBoth"));
            when(valueOps.increment("st:local:rc:instance-service:users:ver")).thenReturn(2L);
            when(valueOps.increment("st:local:rc:instance-service:workspaces:ver")).thenReturn(6L);
            when(pjp.proceed()).thenReturn("ok");
            // act
            Object actual = aspect.aroundEvict(pjp);
            // assert
            assertThat(actual).isEqualTo("ok");
            // verify
            InOrder order = inOrder(valueOps, pjp);
            order.verify(valueOps).increment("st:local:rc:instance-service:users:ver");
            order.verify(pjp).proceed();
            order.verify(valueOps).increment("st:local:rc:instance-service:workspaces:ver");
        }

        @Test
        @DisplayName("does not bump generations when cache is disabled")
        void doesNotBumpGenerationsWhenCacheIsDisabled() throws Throwable {
            // arrange
            // conditions
            when(props.isEnabled()).thenReturn(false);
            when(pjp.proceed()).thenReturn("ok");
            // act
            Object actual = aspect.aroundEvict(pjp);
            // assert
            assertThat(actual).isEqualTo("ok");
            // verify
            verify(pjp).proceed();
            verifyNoInteractions(redis);
        }

    }

    private void enableCacheWriteDefaults() {
        when(props.isEnabled()).thenReturn(true);
        when(props.getKeyPrefix()).thenReturn(KEY_PREFIX);
        when(props.getHashBytes()).thenReturn(12);
    }

    private void enableCacheEvictDefaults() {
        when(props.isEnabled()).thenReturn(true);
        when(props.getKeyPrefix()).thenReturn(KEY_PREFIX);
    }

    private static MockHttpServletRequest bindRequest(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    private static CacheResponse cacheAnnotation(String methodName) throws NoSuchMethodException {
        return method(methodName).getAnnotation(CacheResponse.class);
    }

    private static Method method(String methodName) throws NoSuchMethodException {
        return FixtureEndpoint.class.getDeclaredMethod(methodName);
    }

    private record DemoBody(String name) {
    }

    private static final class FixtureEndpoint {

        @CacheResponse(group = "users", ttlSeconds = 60)
        ResponseEntity<DemoBody> cacheable() {
            return ResponseEntity.ok(new DemoBody("unused"));
        }

        @CacheResponse(group = "users", ttlSeconds = 60, varyHeaders = { "Accept-Language" })
        ResponseEntity<DemoBody> cacheableWithVaryHeader() {
            return ResponseEntity.ok(new DemoBody("unused"));
        }

        @CacheResponse(group = "me", ttlSeconds = 60, perUser = true)
        ResponseEntity<DemoBody> perUserCacheable() {
            return ResponseEntity.ok(new DemoBody("unused"));
        }

        @InvalidateCache(group = "users", beforeInvocation = true)
        @InvalidateCache(group = "workspaces", beforeInvocation = false)
        String evictBoth() {
            return "ok";
        }

    }

}
