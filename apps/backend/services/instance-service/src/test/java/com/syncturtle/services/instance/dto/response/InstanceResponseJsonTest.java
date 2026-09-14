package com.syncturtle.services.instance.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.syncturtle.common.cache.payload.CachedResponsePayload;
import com.syncturtle.common.cache.property.ResponseCacheProperties;
import com.syncturtle.common.cache.response.ResponseCacheAspect;
import com.syncturtle.common.cache.response.ResponseCacheKeyBuilder;
import com.syncturtle.common.cache.response.annotation.CacheResponse;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.services.instance.controller.InstanceController;
import com.syncturtle.services.instance.type.InstanceAdminRole;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Instance response JSON")
class InstanceResponseJsonTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-31T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-08-31T12:30:00Z");

    @Mock
    StringRedisTemplate redis;
    @Mock
    ValueOperations<String, String> valueOperations;
    @Mock
    ProceedingJoinPoint joinPoint;
    @Mock
    MethodSignature methodSignature;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("round trip")
    class RoundTripTests {

        @Test
        @DisplayName("reconstructs the public setup response and its nested graph")
        void reconstructsPublicSetupResponseAndNestedGraph() throws Exception {
            // arrange
            JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
            InstanceSetupConfigResponse config = InstanceSetupConfigResponse.builder()
                    .enableSignup(true)
                    .workspaceCreationDisabled(false)
                    .googleEnabled(true)
                    .githubEnabled(true)
                    .gitlabEnabled(false)
                    .magicLoginEnabled(true)
                    .emailPasswordEnabled(true)
                    .githubAppName("syncturtle")
                    .posthogApiKey("test-posthog-key")
                    .posthogHost("https://posthog.example.test")
                    .unsplashConfigured(true)
                    .fileSizeLimit(25.5)
                    .smtpConfigured(true)
                    .intercomEnabled(false)
                    .intercomAppId("test-intercom-app")
                    .adminBaseUrl("https://admin.example.test")
                    .appBaseUrl("https://app.example.test")
                    .instanceChangelogUrl("https://example.test/changelog")
                    .build();
            InstanceResponse instance = InstanceResponse.builder()
                    .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                    .instanceName("SyncTurtle")
                    .whitelistEmails("example.test")
                    .licenseKey("test-license")
                    .instanceId("instance-test")
                    .currentVersion("0.0.1")
                    .latestVersion("0.0.2")
                    .lastCheckedAt(UPDATED_AT)
                    .namespace("test")
                    .telemetryEnabled(true)
                    .supportRequired(false)
                    .activated(true)
                    .setupDone(true)
                    .signupScreenVisited(true)
                    .verified(true)
                    .workspaceExist(true)
                    .userCount(4L)
                    .createdAt(CREATED_AT)
                    .updatedAt(UPDATED_AT)
                    .createdBy(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                    .updatedBy(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                    .build();
            InstanceSetupResponse expected = InstanceSetupResponse.builder()
                    .isActivated(true)
                    .isSetupDone(true)
                    .config(config)
                    .instance(instance)
                    .build();

            // act
            String json = jsonMapper.writeValueAsString(expected);
            InstanceSetupResponse actual = jsonMapper.readValue(json, InstanceSetupResponse.class);

            // assert
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
            assertThat(jsonMapper.readTree(json).get("config").get("isWorkspaceCreationDisabled").asBoolean())
                    .isFalse();
            assertThat(jsonMapper.readTree(json).get("instance").get("isTelemetryEnabled").asBoolean())
                    .isTrue();
        }

        @Test
        @DisplayName("reconstructs the administrator list and nested user details")
        void reconstructsAdministratorListAndNestedUserDetails() throws Exception {
            // arrange
            JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
            UserAdminLiteResponse userDetail = UserAdminLiteResponse.builder()
                    .id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                    .email("admin@example.test")
                    .firstName("Ada")
                    .lastName("Lovelace")
                    .displayName("Ada Lovelace")
                    .avatarUrl("https://example.test/avatar.png")
                    .dateJoined(CREATED_AT)
                    .build();
            InstanceAdminResponse admin = InstanceAdminResponse.builder()
                    .id(UUID.fromString("55555555-5555-5555-5555-555555555555"))
                    .instance(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                    .user(userDetail.getId())
                    .role(InstanceAdminRole.ADMIN)
                    .createdAt(CREATED_AT)
                    .updatedAt(UPDATED_AT)
                    .userDetail(userDetail)
                    .createdBy(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                    .updatedBy(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                    .build();
            List<InstanceAdminResponse> expected = List.of(admin);
            JavaType responseType = jsonMapper.getTypeFactory()
                    .constructCollectionType(List.class, InstanceAdminResponse.class);

            // act
            String json = jsonMapper.writeValueAsString(expected);
            List<InstanceAdminResponse> actual = jsonMapper.readValue(json, responseType);

            // assert
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        }

        @Test
        @DisplayName("reconstructs the configuration list")
        void reconstructsConfigurationList() throws Exception {
            // arrange
            JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
            InstanceConfigurationResponse configuration = InstanceConfigurationResponse.builder()
                    .id(UUID.fromString("66666666-6666-6666-6666-666666666666"))
                    .key(InstanceConfigurationKey.ENABLE_SIGNUP)
                    .value("1")
                    .createdAt(CREATED_AT)
                    .updatedAt(UPDATED_AT)
                    .createdById(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                    .updatedById(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                    .build();
            List<InstanceConfigurationResponse> expected = List.of(configuration);
            JavaType responseType = jsonMapper.getTypeFactory()
                    .constructCollectionType(List.class, InstanceConfigurationResponse.class);

            // act
            String json = jsonMapper.writeValueAsString(expected);
            List<InstanceConfigurationResponse> actual = jsonMapper.readValue(json, responseType);

            // assert
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("response cache")
    class ResponseCacheTests {

        @Test
        @DisplayName("reads the payload written on a miss without deleting or recomputing on the hit")
        void readsWrittenPayloadWithoutDeletingOrRecomputing() throws Throwable {
            // arrange
            JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
            ResponseCacheProperties properties = new ResponseCacheProperties(
                    true,
                    "st:test:rc:",
                    12,
                    "com.syncturtle.",
                    "X-Workspace-Id");
            ResponseCacheAspect aspect = new ResponseCacheAspect(
                    redis,
                    jsonMapper,
                    properties,
                    new ResponseCacheKeyBuilder(),
                    null,
                    "instance-service");
            InstanceSetupResponse expectedBody = InstanceSetupResponse.builder()
                    .isActivated(true)
                    .isSetupDone(false)
                    .config(InstanceSetupConfigResponse.builder()
                            .enableSignup(true)
                            .workspaceCreationDisabled(false)
                            .appBaseUrl("https://app.example.test")
                            .adminBaseUrl("https://admin.example.test")
                            .build())
                    .instance(InstanceResponse.builder()
                            .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                            .instanceName("SyncTurtle")
                            .instanceId("instance-test")
                            .createdAt(CREATED_AT)
                            .updatedAt(UPDATED_AT)
                            .build())
                    .build();
            ResponseEntity<InstanceSetupResponse> computedResponse = ResponseEntity.ok(expectedBody);
            Method controllerMethod = InstanceController.class.getMethod("getInstanceSetupInfo");
            CacheResponse annotation = controllerMethod.getAnnotation(CacheResponse.class);
            AtomicReference<String> storedPayload = new AtomicReference<>();

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/instances");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            when(redis.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0, String.class);
                if (key.endsWith(":ver")) {
                    return "1";
                }
                return storedPayload.get();
            });
            doAnswer(invocation -> {
                storedPayload.set(invocation.getArgument(1, String.class));
                return null;
            }).when(valueOperations).set(anyString(), anyString(), eq(Duration.ofHours(2)));
            when(joinPoint.proceed()).thenReturn(computedResponse);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(controllerMethod);

            // act
            Object missResult = aspect.aroundCache(joinPoint, annotation);
            Object hitResult = aspect.aroundCache(joinPoint, annotation);

            // assert
            assertThat(missResult).isSameAs(computedResponse);
            assertThat(storedPayload.get()).isNotBlank();
            CachedResponsePayload cached = jsonMapper.readValue(storedPayload.get(), CachedResponsePayload.class);
            assertThat(cached.getBodyType()).isEqualTo(InstanceSetupResponse.class.getName());
            assertThat(hitResult).isInstanceOf(ResponseEntity.class);
            ResponseEntity<?> cachedResponse = (ResponseEntity<?>) hitResult;
            assertThat(cachedResponse.getStatusCode().value()).isEqualTo(200);
            assertThat(cachedResponse.getBody()).usingRecursiveComparison().isEqualTo(expectedBody);

            // verify
            verify(joinPoint, times(1)).proceed();
            verify(redis, never()).delete(anyString());
            verify(valueOperations, times(1))
                    .set(anyString(), anyString(), eq(Duration.ofHours(2)));
        }
    }

}
