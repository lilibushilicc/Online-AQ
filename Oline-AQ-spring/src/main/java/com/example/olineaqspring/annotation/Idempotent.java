package com.example.olineaqspring.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    String prefix() default "";
    int ttlSeconds() default 300;
}
