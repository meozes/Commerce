package kr.hhplus.be.server.common.aop.annotation;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String lockName() ; // 기본 락 이름
    long key() default 0L;
    long waitTime() default 5L; // 락 획득 대기 시간 (초)
    long leaseTime() default 10L; // 락 점유 시간 (초)
}
