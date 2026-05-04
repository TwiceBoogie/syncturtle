package com.syncturtle.common.spring.cache.response;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseCache {
    long ttlSeconds() default 3600;

    String group();

    boolean perUser() default true;

    boolean perWorkspace() default false;
}
