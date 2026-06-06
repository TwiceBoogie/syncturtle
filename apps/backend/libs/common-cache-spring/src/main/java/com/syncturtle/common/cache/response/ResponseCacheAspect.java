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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.cache.payload.CachedResponsePayload;
import com.syncturtle.common.cache.properties.ResponseCacheProperties;
import com.syncturtle.common.cache.response.annotation.CacheResponse;
import com.syncturtle.common.cache.response.annotation.InvalidateCache;
import com.syncturtle.common.web.context.RequestUserContext;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
public final class ResponseCacheAspect {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ResponseCacheProperties props;
    private final ResponseCacheKeyBuilder keyBuilder;
    private final RequestUserContext userContext;
    private final String serviceName;

    public ResponseCacheAspect(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            ResponseCacheProperties props,
            ResponseCacheKeyBuilder keyBuilder,
            RequestUserContext userContext,
            String serviceName) {
        Assert.notNull(redis, "redis is required");
        Assert.notNull(objectMapper, "objectMapper is required");
        Assert.notNull(props, "props is required");
        Assert.notNull(keyBuilder, "keyBuilder is required");
        Assert.notNull(userContext, "userContext is required");
        Assert.hasText(serviceName, "serviceName is required");

        this.redis = redis;
        this.objectMapper = objectMapper;
        this.props = props;
        this.keyBuilder = keyBuilder;
        this.userContext = userContext;
        this.serviceName = serviceName.trim();
    }

    @Around("@annotation(cacheAnn)")
    public Object aroundCache(ProceedingJoinPoint pjp, CacheResponse cacheAnn) throws Throwable {
        if (!props.isEnabled()) {
            return pjp.proceed();
        }

        HttpServletRequest request = currentRequestOrNull();
        if (request == null) {
            return pjp.proceed();
        }

        if (!isSafeHttpMethod(request)) {
            return pjp.proceed();
        }

        if (cacheAnn.ttlSeconds() <= 0) {
            return pjp.proceed();
        }

        String group = cacheAnn.group();

        String userScope = resolveUserScope(cacheAnn);
        if (cacheAnn.perUser() && userScope == null) {
            return pjp.proceed();
        }

        String workspaceScope = resolveWorkspaceScope(request, cacheAnn);
        if (cacheAnn.perWorkspace() && workspaceScope == null) {
            return pjp.proceed();
        }

        String generation = currentGeneration(group);

        String canonicalVariant = keyBuilder.canonicalVariantInput(request, cacheAnn.varyHeaders());
        String hash = keyBuilder.variantHashHex(canonicalVariant, props.getHashBytes());

        String cacheKey = keyBuilder.responseKey(props.getKeyPrefix(), serviceName, group, generation, hash,
                workspaceScope, userScope);

        String cachedJson = redis.opsForValue().get(cacheKey);
        if (cachedJson != null) {
            ResponseEntity<?> cachedResponse = readCachedResponse(pjp, cacheKey, cachedJson);
            if (cachedResponse != null) {
                log.debug("Response cache hit: {}", cacheKey);
                return cachedResponse;
            }

            redis.delete(cacheKey);
        }

        log.debug("Response cache miss: {}", cacheKey);
        return proceedAndMaybeCache(pjp, cacheAnn, cacheKey);
    }

    @Around("@annotation(com.syncturtle.common.cache.response.annotation.InvalidateCache) || " +
            "@annotation(com.syncturtle.common.cache.response.annotation.InvalidateCaches)")
    public Object aroundEvict(ProceedingJoinPoint pjp) throws Throwable {
        if (!props.isEnabled()) {
            return pjp.proceed();
        }

        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        InvalidateCache[] evicts = method.getAnnotationsByType(InvalidateCache.class);

        // bump all that want "before"
        for (InvalidateCache evict : evicts) {
            if (evict.beforeInvocation()) {
                bumpGeneration(evict.group());
            }
        }

        Object result = pjp.proceed();

        // bump all that want "after"
        for (InvalidateCache evict : evicts) {
            if (!evict.beforeInvocation()) {
                bumpGeneration(evict.group());
            }
        }

        return result;
    }

    private Object proceedAndMaybeCache(ProceedingJoinPoint pjp, CacheResponse annotation, String cacheKey)
            throws Throwable {
        Object result = pjp.proceed();

        if (!(result instanceof ResponseEntity<?> response)) {
            return result;
        }

        int status = response.getStatusCode().value();
        if (!isCacheableStatus(status, annotation.cacheableStatuses())) {
            return result;
        }

        Object body = response.getBody();

        CachedResponsePayload payload = new CachedResponsePayload();
        payload.setStatus(status);

        if (body == null) {
            payload.setBodyJson(null);
            payload.setBodyType(null);
        } else {
            payload.setBodyJson(objectMapper.writeValueAsString(body));
            payload.setBodyType(body.getClass().getName());
        }

        String json = objectMapper.writeValueAsString(payload);
        redis.opsForValue().set(cacheKey, json, Duration.ofSeconds(annotation.ttlSeconds()));

        log.debug("Response cached: {}", cacheKey);

        return result;
    }

    private ResponseEntity<?> readCachedResponse(ProceedingJoinPoint pjp, String cacheKey, String cachedJson) {
        try {
            CachedResponsePayload cached = objectMapper.readValue(cachedJson, CachedResponsePayload.class);

            if (cached.getBodyType() == null) {
                return ResponseEntity.status(cached.getStatus()).body(null);
            }

            JavaType declaredBodyType = resolveResponseEntityBodyType(pjp);
            JavaType deserializeAs = chooseDeserializationType(declaredBodyType, cached.getBodyType());

            if (deserializeAs == null) {
                log.warn("Ignoring cached response because body type is not safe. key={}, bodyType={}", cacheKey,
                        cached.getBodyType());
                return null;
            }

            Object body = objectMapper.readValue(cached.getBodyJson(), deserializeAs);

            return ResponseEntity.status(cached.getStatus()).body(body);
        } catch (Exception exception) {
            log.warn("Failed to read cached response. key={}", cacheKey, exception);
            return null;
        }
    }

    private String currentGeneration(String group) {
        String versionKey = keyBuilder.versionKey(props.getKeyPrefix(), serviceName, group);

        String existingGeneration = redis.opsForValue().get(versionKey);
        if (existingGeneration != null) {
            return existingGeneration;
        }

        Boolean initialized = redis.opsForValue().setIfAbsent(versionKey, "1");
        if (Boolean.TRUE.equals(initialized)) {
            return "1";
        }

        String genCreatedByAnotherRequest = redis.opsForValue().get(versionKey);
        if (genCreatedByAnotherRequest != null) {
            return genCreatedByAnotherRequest;
        }

        return "1";
    }

    private void bumpGeneration(String group) {
        String versionKey = keyBuilder.versionKey(props.getKeyPrefix(), serviceName, group);
        Long generation = redis.opsForValue().increment(versionKey);

        log.debug("response cache generation bumped. key={}, generation={}", versionKey, generation);
    }

    private String resolveUserScope(CacheResponse annotation) {
        if (!annotation.perUser()) {
            return "";
        }

        if (userContext == null || userContext.getUserId() == null) {
            return null;
        }

        return keyBuilder.userScope(userContext.getUserId().toString());
    }

    private String resolveWorkspaceScope(HttpServletRequest request, CacheResponse annotation) {
        if (!annotation.perWorkspace()) {
            return "";
        }

        String workspaceId = request.getHeader(props.getWorkspaceHeaderName());
        if (!StringUtils.hasText(workspaceId)) {
            return null;
        }

        return keyBuilder.workspaceScope(workspaceId);
    }

    private HttpServletRequest currentRequestOrNull() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private JavaType resolveResponseEntityBodyType(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Type returnType = sig.getMethod().getGenericReturnType();

        if (!(returnType instanceof ParameterizedType pt)) {
            return null;
        }
        Type raw = pt.getRawType();
        if (!(raw instanceof Class<?> rawClass)) {
            return null;
        }
        if (!ResponseEntity.class.isAssignableFrom(rawClass)) {
            return null;
        }
        Type body = pt.getActualTypeArguments()[0];
        return objectMapper.getTypeFactory().constructType(body);
    }

    private JavaType chooseDeserializationType(JavaType declaredBodyType, String cachedBodyTypeName) {
        if (!StringUtils.hasText(cachedBodyTypeName)) {
            return null;
        }

        if (declaredBodyType == null) {
            return safeTypeFromCache(null, cachedBodyTypeName);
        }

        Class<?> declaredRaw = declaredBodyType.getRawClass();

        if (Collection.class.isAssignableFrom(declaredRaw)) {
            return declaredBodyType;
        }

        if (Map.class.isAssignableFrom(declaredRaw)) {
            return declaredBodyType;
        }

        boolean declaredIsInterfaceOrAbstract = declaredRaw.isInterface()
                || Modifier.isAbstract(declaredRaw.getModifiers());

        if (!declaredIsInterfaceOrAbstract) {
            if (!declaredRaw.getName().equals(cachedBodyTypeName)) {
                return null;
            }
            return declaredBodyType;
        }

        return safeTypeFromCache(declaredRaw, cachedBodyTypeName);
    }

    private JavaType safeTypeFromCache(Class<?> declaredBase, String cachedBodyTypeName) {
        if (!StringUtils.hasText(cachedBodyTypeName)) {
            return null;
        }

        // allowlist
        if (!cachedBodyTypeName.startsWith(props.getBodyTypeAllowlistPrefix())) {
            return null;
        }

        try {
            Class<?> concrete = Class.forName(cachedBodyTypeName);

            if (declaredBase != null && !declaredBase.isAssignableFrom(concrete)) {
                return null;
            }

            return objectMapper.getTypeFactory().constructType(concrete);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    private static boolean isSafeHttpMethod(HttpServletRequest request) {
        String method = request.getMethod();
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
    }

    private static boolean isCacheableStatus(int status, int[] cacheableStatuses) {
        if (cacheableStatuses == null || cacheableStatuses.length == 0) {
            return status == 200;
        }

        return Arrays.stream(cacheableStatuses).anyMatch(candidate -> candidate == status);
    }
}
