package com.example.blog.common.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 선언적 멱등성 보장을 위한 커스텀 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    // 멱등성 키 헤더 이름 (기본값: Idempotency-Key)
    String headerName() default "Idempotency-Key";

    // 캐시 만료 시간 (기본값: 120초)
    long ttl() default 120;

    // 만료 시간 단위 (기본값: SECONDS)
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
