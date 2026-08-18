package com.example.blog.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Aspect
@Component
public class IdempotencyAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyAspect(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(idempotent)")
    public Object execute(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = getCurrentHttpRequest();
        String idempotencyKey = request.getHeader(idempotent.headerName());

        // 1. 헤더에 멱등성 키가 없는 경우 비즈니스 로직을 그대로 통과시킵니다.
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return joinPoint.proceed();
        }

        String redisKey = "idempotency:" + idempotencyKey.trim();
        Duration ttlDuration = Duration.of(idempotent.ttl(), idempotent.timeUnit().toChronoUnit());

        // 2. Redis SET NX를 통해 원자적으로 IN_PROGRESS 상태를 선점합니다.
        IdempotencyRecord inProgressRecord = IdempotencyRecord.inProgress();
        Boolean isAcquired = redisTemplate.opsForValue().setIfAbsent(redisKey, inProgressRecord, ttlDuration);

        if (Boolean.FALSE.equals(isAcquired)) {
            // 키가 이미 존재하는 경우: 이전 요청의 상태를 확인합니다.
            Object existingValue = redisTemplate.opsForValue().get(redisKey);
            IdempotencyRecord record = objectMapper.convertValue(existingValue, IdempotencyRecord.class);

            if (record != null && record.getStatus() == IdempotencyRecord.Status.IN_PROGRESS) {
                // 현재 다른 스레드나 프로세스에서 처리 중인 경우 동시 요청 충돌을 알립니다.
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("해당 요청이 현재 처리 중입니다. 잠시 후 결과를 다시 확인하십시오.");
            }

            if (record != null && record.getStatus() == IdempotencyRecord.Status.COMPLETED) {
                // 이미 완료된 요청인 경우 직전 응답 객체를 즉시 반환하여 멱등성을 보장합니다.
                return ResponseEntity.status(record.getStatusCode()).body(record.getResponseBody());
            }
        }

        // 3. 비즈니스 로직 실행 및 결과 캐싱
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            // 로직 실행 중 예외가 발생하면 락을 즉시 해제하여 클라이언트의 즉각적인 재시도를 허용합니다.
            redisTemplate.delete(redisKey);
            throw ex;
        }

        // 4. 정상 종료 시 응답 상태코드 및 본문을 Redis에 COMPLETED 상태로 갱신 적재합니다.
        int statusCode = HttpStatus.OK.value();
        Object responseBody = result;

        if (result instanceof ResponseEntity<?> responseEntity) {
            statusCode = responseEntity.getStatusCode().value();
            responseBody = responseEntity.getBody();
        }

        IdempotencyRecord completedRecord = IdempotencyRecord.completed(statusCode, responseBody);
        redisTemplate.opsForValue().set(redisKey, completedRecord, ttlDuration);

        return result;
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("HTTP 요청 컨텍스트를 찾을 수 없습니다.");
        }
        return attributes.getRequest();
    }
}
