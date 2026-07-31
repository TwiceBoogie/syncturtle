package com.syncturtle.testing.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseKafka {
    /**
     * Optional explicit consumer group. If blank, test-support derives one from the
     * test class name.
     * 
     * @return
     */
    String consumerGroup() default "";

    /**
     * Optional explicit config-broadcast group. If blank, test-support derives one
     * from the test class name.
     * 
     * @return
     */
    String configBroadcastGroup() default "";

}
