package com.syncturtle.common.cache.response.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheResponse {

    /**
     * Business invalidation group
     */
    String group();

    /**
     * TTL for actual response data keys.
     * 
     * Version keys do not expire
     */
    long ttlSeconds() default 3600;

    /**
     * Adds u:{userId} to the key.
     * 
     * If enabled but no user id exists, the response is not cached
     */
    boolean perUser() default false;

    /**
     * Adds w:{workspaceId} to the key.
     * 
     * If enabled but no workspace id exists, the response is not cached
     */
    boolean perWorkspace() default false;

    /**
     * Optional headers that should affect the cache variant.
     * 
     * Example:
     * varyHeaders = {"Accept-Language"}
     */
    String[] varyHeaders() default {};

    /**
     * Status code allowed to be cached.
     * 
     * Default: only 200
     */
    int[] cacheableStatuses() default { 200 };
}
