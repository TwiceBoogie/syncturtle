package com.syncturtle.common.cache.response;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.syncturtle.common.cache.payload.CachedResponsePayload;
import com.syncturtle.common.cache.properties.ResponseCacheProperties;
import com.syncturtle.common.cache.response.annotation.CacheResponse;
import com.syncturtle.common.cache.response.annotation.InvalidateCache;
import com.syncturtle.common.web.context.RequestUserContext;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Aspect
public final class ResponseCacheAspect {

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;
    private final ResponseCacheProperties properties;
    private final ResponseCacheKeyBuilder keyBuilder;
    private final RequestUserContext userContext;
    private final String serviceName;

    public ResponseCacheAspect(
            StringRedisTemplate redis,
            JsonMapper jsonMapper,
            ResponseCacheProperties properties,
            ResponseCacheKeyBuilder keyBuilder,
            RequestUserContext userContext,
            String serviceName) {
        Assert.notNull(redis, "redis is required");
        Assert.notNull(jsonMapper, "jsonMapper is required");
        Assert.notNull(properties, "properties is required");
        Assert.notNull(keyBuilder, "keyBuilder is required");
        Assert.hasText(serviceName, "serviceName is required");

        this.redis = redis;
        this.jsonMapper = jsonMapper;
        this.properties = properties;
        this.keyBuilder = keyBuilder;
        this.userContext = userContext;
        this.serviceName = serviceName.trim();
    }

    @Around("@annotation(cacheResponse)")
    public Object aroundCache(
            ProceedingJoinPoint joinPoint,
            CacheResponse cacheResponse) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = currentRequestOrNull();

        if (request == null) {
            return joinPoint.proceed();
        }

        if (!isSafeHttpMethod(request)) {
            return joinPoint.proceed();
        }

        if (cacheResponse.ttlSeconds() <= 0) {
            return joinPoint.proceed();
        }

        String group = cacheResponse.group();

        String userScope = resolveUserScope(cacheResponse);

        if (cacheResponse.perUser() && userScope == null) {
            return joinPoint.proceed();
        }

        String workspaceScope = resolveWorkspaceScope(
                request,
                cacheResponse);

        if (cacheResponse.perWorkspace() && workspaceScope == null) {
            return joinPoint.proceed();
        }

        String generation = currentGeneration(group);

        String canonicalVariant = keyBuilder.canonicalVariantInput(
                request,
                cacheResponse.varyHeaders());

        String hash = keyBuilder.variantHashHex(
                canonicalVariant,
                properties.getHashBytes());

        String cacheKey = keyBuilder.responseKey(
                properties.getKeyPrefix(),
                serviceName,
                group,
                generation,
                hash,
                workspaceScope,
                userScope);

        String cachedJson = redis.opsForValue().get(cacheKey);

        if (cachedJson != null) {
            ResponseEntity<?> cachedResponse = readCachedResponse(
                    joinPoint,
                    cacheKey,
                    cachedJson);

            if (cachedResponse != null) {
                log.debug("Response cache hit: {}", cacheKey);
                return cachedResponse;
            }

            redis.delete(cacheKey);
        }

        log.debug("Response cache miss: {}", cacheKey);

        return proceedAndMaybeCache(
                joinPoint,
                cacheResponse,
                cacheKey);
    }

    @Around("@annotation("
            + "com.syncturtle.common.cache.response.annotation.InvalidateCache"
            + ") || @annotation("
            + "com.syncturtle.common.cache.response.annotation.InvalidateCaches"
            + ")")
    public Object aroundEvict(
            ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        Method method = ((MethodSignature) joinPoint.getSignature())
                .getMethod();

        InvalidateCache[] invalidations = method.getAnnotationsByType(InvalidateCache.class);

        for (InvalidateCache invalidation : invalidations) {
            if (invalidation.beforeInvocation()) {
                bumpGeneration(invalidation.group());
            }
        }

        Object result = joinPoint.proceed();

        for (InvalidateCache invalidation : invalidations) {
            if (!invalidation.beforeInvocation()) {
                bumpGeneration(invalidation.group());
            }
        }

        return result;
    }

    private Object proceedAndMaybeCache(
            ProceedingJoinPoint joinPoint,
            CacheResponse annotation,
            String cacheKey) throws Throwable {
        Object result = joinPoint.proceed();

        if (!(result instanceof ResponseEntity<?> response)) {
            return result;
        }

        int status = response.getStatusCode().value();

        if (!isCacheableStatus(
                status,
                annotation.cacheableStatuses())) {
            return result;
        }

        Object body = response.getBody();

        CachedResponsePayload payload = new CachedResponsePayload();

        payload.setStatus(status);

        if (body == null) {
            payload.setBodyJson(null);
            payload.setBodyType(null);
        } else {
            payload.setBodyJson(
                    jsonMapper.writeValueAsString(body));

            payload.setBodyType(body.getClass().getName());
        }

        String payloadJson = jsonMapper.writeValueAsString(payload);

        redis.opsForValue().set(
                cacheKey,
                payloadJson,
                Duration.ofSeconds(annotation.ttlSeconds()));

        log.debug("Response cached: {}", cacheKey);

        return result;
    }

    private ResponseEntity<?> readCachedResponse(
            ProceedingJoinPoint joinPoint,
            String cacheKey,
            String cachedJson) {
        try {
            CachedResponsePayload cached = jsonMapper.readValue(
                    cachedJson,
                    CachedResponsePayload.class);

            if (cached.getBodyType() == null) {
                return ResponseEntity
                        .status(cached.getStatus())
                        .body(null);
            }

            JavaType declaredBodyType = resolveResponseEntityBodyType(joinPoint);

            JavaType deserializationType = chooseDeserializationType(
                    declaredBodyType,
                    cached.getBodyType());

            if (deserializationType == null) {
                log.warn(
                        "Ignoring cached response because body type is "
                                + "not safe. key={}, bodyType={}",
                        cacheKey,
                        cached.getBodyType());

                return null;
            }

            Object body = jsonMapper.readValue(
                    cached.getBodyJson(),
                    deserializationType);

            return ResponseEntity
                    .status(cached.getStatus())
                    .body(body);
        } catch (Exception exception) {
            log.warn(
                    "Failed to read cached response. key={}",
                    cacheKey,
                    exception);

            return null;
        }
    }

    private String currentGeneration(String group) {
        String versionKey = keyBuilder.versionKey(
                properties.getKeyPrefix(),
                serviceName,
                group);

        String existingGeneration = redis.opsForValue().get(versionKey);

        if (existingGeneration != null) {
            return existingGeneration;
        }

        Boolean initialized = redis.opsForValue()
                .setIfAbsent(versionKey, "1");

        if (Boolean.TRUE.equals(initialized)) {
            return "1";
        }

        String generationCreatedByAnotherRequest = redis.opsForValue().get(versionKey);

        if (generationCreatedByAnotherRequest != null) {
            return generationCreatedByAnotherRequest;
        }

        return "1";
    }

    private void bumpGeneration(String group) {
        String versionKey = keyBuilder.versionKey(
                properties.getKeyPrefix(),
                serviceName,
                group);

        Long generation = redis.opsForValue().increment(versionKey);

        log.debug(
                "Response cache generation bumped. key={}, generation={}",
                versionKey,
                generation);
    }

    private String resolveUserScope(
            CacheResponse annotation) {
        if (!annotation.perUser()) {
            return "";
        }

        if (userContext == null
                || userContext.getUserId() == null) {
            return null;
        }

        return keyBuilder.userScope(
                userContext.getUserId().toString());
    }

    private String resolveWorkspaceScope(
            HttpServletRequest request,
            CacheResponse annotation) {
        if (!annotation.perWorkspace()) {
            return "";
        }

        String workspaceId = request.getHeader(
                properties.getWorkspaceHeaderName());

        if (!StringUtils.hasText(workspaceId)) {
            return null;
        }

        return keyBuilder.workspaceScope(workspaceId);
    }

    private HttpServletRequest currentRequestOrNull() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }

        return null;
    }

    private JavaType resolveResponseEntityBodyType(
            ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        Type returnType = signature
                .getMethod()
                .getGenericReturnType();

        if (!(returnType instanceof ParameterizedType parameterizedType)) {
            return null;
        }

        Type rawType = parameterizedType.getRawType();

        if (!(rawType instanceof Class<?> rawClass)) {
            return null;
        }

        if (!ResponseEntity.class.isAssignableFrom(rawClass)) {
            return null;
        }

        Type bodyType = parameterizedType.getActualTypeArguments()[0];

        return jsonMapper
                .getTypeFactory()
                .constructType(bodyType);
    }

    private JavaType chooseDeserializationType(
            JavaType declaredBodyType,
            String cachedBodyTypeName) {
        if (!StringUtils.hasText(cachedBodyTypeName)) {
            return null;
        }

        if (declaredBodyType == null) {
            return safeTypeFromCache(
                    null,
                    cachedBodyTypeName);
        }

        Class<?> declaredRawClass = declaredBodyType.getRawClass();

        if (Collection.class.isAssignableFrom(declaredRawClass)) {
            return declaredBodyType;
        }

        if (Map.class.isAssignableFrom(declaredRawClass)) {
            return declaredBodyType;
        }

        boolean interfaceOrAbstract = declaredRawClass.isInterface()
                || Modifier.isAbstract(
                        declaredRawClass.getModifiers());

        if (!interfaceOrAbstract) {
            if (!declaredRawClass
                    .getName()
                    .equals(cachedBodyTypeName)) {
                return null;
            }

            return declaredBodyType;
        }

        return safeTypeFromCache(
                declaredRawClass,
                cachedBodyTypeName);
    }

    private JavaType safeTypeFromCache(
            Class<?> declaredBase,
            String cachedBodyTypeName) {
        if (!StringUtils.hasText(cachedBodyTypeName)) {
            return null;
        }

        if (!cachedBodyTypeName.startsWith(
                properties.getBodyTypeAllowlistPrefix())) {
            return null;
        }

        try {
            Class<?> concreteType = Class.forName(cachedBodyTypeName);

            if (declaredBase != null
                    && !declaredBase.isAssignableFrom(concreteType)) {
                return null;
            }

            return jsonMapper
                    .getTypeFactory()
                    .constructType(concreteType);
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    private static boolean isSafeHttpMethod(
            HttpServletRequest request) {
        String method = request.getMethod();

        return "GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method);
    }

    private static boolean isCacheableStatus(
            int status,
            int[] cacheableStatuses) {
        if (cacheableStatuses == null
                || cacheableStatuses.length == 0) {
            return status == 200;
        }

        return Arrays.stream(cacheableStatuses)
                .anyMatch(candidate -> candidate == status);
    }
}