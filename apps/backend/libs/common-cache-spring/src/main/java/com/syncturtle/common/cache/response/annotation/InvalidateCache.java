package com.syncturtle.common.cache.response.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(InvalidateCaches.class)
public @interface InvalidateCache {
    /**
     * which cached endpoint group to invalidate (version bump)
     */
    String group();

    /**
     * true = bump generation before method runs.
     * false = bump generation after method succeeds.
     */
    boolean beforeInvocation() default true;
}
