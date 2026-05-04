package com.syncturtle.common.spring.cache.response;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ResponseCacheEvicts.class)
public @interface ResponseCacheEvict {
    /**
     * which cached endpoint group to invalidate (version bump)
     */
    String group();

    /**
     * invalidates before calling handler
     */
    boolean beforeInvocation() default true;
}
