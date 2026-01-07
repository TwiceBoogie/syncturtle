package com.syncturtle.common.spring.cache.response;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.spring.properties.ResponseCacheProperties;
import com.syncturtle.common.web.context.RequestUserContext;

import jakarta.servlet.http.HttpServletRequest;

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
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.props = props;
        this.keyBuilder = keyBuilder;
        this.userContext = userContext;
        this.serviceName = serviceName;
    }

    @Around("@annotation(cacheAnn)")
    public Object aroundCache(ProceedingJoinPoint pjp, ResponseCache cacheAnn) throws Throwable {
        if (!props.isEnabled()) {
            return pjp.proceed();
        }

        HttpServletRequest request = currentRequestOrNull();
        if (request == null) {
            return pjp.proceed();
        }

        String group = cacheAnn.group();

        // 1: read current group version (O(1) invalidation strategy)
        String verKey = keyBuilder.versionKey(props.getKeyPrefix(), serviceName, group);
        String version = redis.opsForValue().get(verKey);
        if (version == null) {
            Boolean ok = redis.opsForValue().setIfAbsent(verKey, "1");
            version = Boolean.TRUE.equals(ok) ? "1" : redis.opsForValue().get(verKey);
            if (version == null) {
                version = "1";
            }
        }

        // 2: build variant hash from canonical request (URI + canonical query)
        String canonicalVariant = keyBuilder.canonicalVariantInput(request);
        String hash = keyBuilder.variantHashHex(canonicalVariant, props.getHashBytes());

        // 3: scopes
        String userScope = "";
        if (cacheAnn.perUser() && userContext != null && userContext.getUserId() != null) {
            userScope = "u:" + userContext.getUserId();
        }

        String workspaceScope = "";
        if (cacheAnn.perWorkspace()) {
            // TODO: add workspace context
        }

        String cacheKey = keyBuilder.responseKey(props.getKeyPrefix(), serviceName, group, version, hash,
                workspaceScope, userScope);

        // 4: cache hit
        String cachedJson = redis.opsForValue().get(cacheKey);
        if (cachedJson != null) {
            CachedHttpResponse cached = objectMapper.readValue(cachedJson, CachedHttpResponse.class);

            JavaType declaredBodyType = resolveResponseEntityBodyType(pjp);
            JavaType deserializesAs = chooseDeserializationType(declaredBodyType, cached.getBodyType());

            Object body = (deserializesAs == null) ? cached.getBody()
                    : objectMapper.readValue(cached.getBody(), deserializesAs);

            return ResponseEntity.status(cached.getStatus()).body(body);
        }

        // 5: miss proceed and cache only if 200
        Object result = pjp.proceed();

        if (result instanceof ResponseEntity<?> re) {
            int status = re.getStatusCode().value();

            if (status == 200) {
                Object bodyObj = re.getBody();

                CachedHttpResponse toCache = new CachedHttpResponse();
                toCache.setStatus(status);

                if (bodyObj == null) {
                    toCache.setBody("null");
                    toCache.setBodyType(null);
                } else {
                    toCache.setBody(objectMapper.writeValueAsString(bodyObj));
                    toCache.setBodyType(bodyObj.getClass().getName());
                }

                String toCacheJson = objectMapper.writeValueAsString(toCache);

                redis.opsForValue().set(cacheKey, toCacheJson, Duration.ofSeconds(cacheAnn.ttlSeconds()));
            }
        }

        return result;
    }

    @Around("@annotation(evictAnn)")
    public Object aroundEvict(ProceedingJoinPoint pjp, ResponseCacheEvict evictAnn) throws Throwable {
        if (!props.isEnabled()) {
            return pjp.proceed();
        }

        String group = evictAnn.group();
        String verKey = keyBuilder.versionKey(props.getKeyPrefix(), serviceName, group);

        if (evictAnn.beforeInvocation()) {
            redis.opsForValue().increment(verKey);
        }

        Object result = pjp.proceed();

        if (!evictAnn.beforeInvocation()) {
            redis.opsForValue().increment(verKey);
        }

        return result;
    }

    private HttpServletRequest currentRequestOrNull() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
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
        if (declaredBodyType == null) {
            return safeTypeFromCache(null, cachedBodyTypeName);
        }

        Class<?> declaredRaw = declaredBodyType.getRawClass();
        boolean declaredIsInterfaceOrAbstract = declaredRaw.isInterface()
                || Modifier.isAbstract(declaredRaw.getModifiers());

        if (!declaredIsInterfaceOrAbstract) {
            return declaredBodyType;
        }

        return safeTypeFromCache(declaredRaw, cachedBodyTypeName);
    }

    private JavaType safeTypeFromCache(Class<?> declaredBase, String cachedBodyTypeName) {
        if (cachedBodyTypeName == null || cachedBodyTypeName.isBlank()) {
            return null;
        }

        // allowlist
        if (!cachedBodyTypeName.startsWith("com.syncturtle.")) {
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
}
