package com.example.blog.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Idempotent idempotent;

    private ObjectMapper objectMapper;
    private IdempotencyAspect idempotencyAspect;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        idempotencyAspect = new IdempotencyAspect(redisTemplate, objectMapper);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("멱등성 키 헤더가 없는 경우 AOP는 Redis 조회 없이 비즈니스 로직을 바로 실행한다")
    void execute_WithoutIdempotencyKey_ShouldProceedDirectly() throws Throwable {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        given(idempotent.headerName()).willReturn("Idempotency-Key");
        given(joinPoint.proceed()).willReturn("NORMAL_RESULT");

        // when
        Object result = idempotencyAspect.execute(joinPoint, idempotent);

        // then
        assertThat(result).isEqualTo("NORMAL_RESULT");
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("최초 요청(Key 없음) 인입 시 IN_PROGRESS 선점 후 정상 종료 시 COMPLETED 상태와 결과를 캐싱한다")
    void execute_FirstRequest_ShouldAcquireLockAndCacheResult() throws Throwable {
        // given
        String key = "test-uuid-1234";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Idempotency-Key", key);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        given(idempotent.headerName()).willReturn("Idempotency-Key");
        given(idempotent.ttl()).willReturn(120L);
        given(idempotent.timeUnit()).willReturn(TimeUnit.SECONDS);

        given(valueOperations.setIfAbsent(eq("idempotency:" + key), any(IdempotencyRecord.class), any(Duration.class)))
                .willReturn(Boolean.TRUE);

        ResponseEntity<Map<String, String>> responseEntity = ResponseEntity.ok(Map.of("status", "SUCCESS"));
        given(joinPoint.proceed()).willReturn(responseEntity);

        // when
        Object result = idempotencyAspect.execute(joinPoint, idempotent);

        // then
        assertThat(result).isEqualTo(responseEntity);
        verify(valueOperations).set(eq("idempotency:" + key), any(IdempotencyRecord.class), eq(Duration.ofSeconds(120)));
    }

    @Test
    @DisplayName("동일 키가 IN_PROGRESS 상태로 존재할 때 동시 요청 인입 시 409 CONFLICT를 반환한다")
    void execute_WhenInProgress_ShouldReturn409Conflict() throws Throwable {
        // given
        String key = "test-uuid-inprogress";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Idempotency-Key", key);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        given(idempotent.headerName()).willReturn("Idempotency-Key");
        given(idempotent.ttl()).willReturn(120L);
        given(idempotent.timeUnit()).willReturn(TimeUnit.SECONDS);

        given(valueOperations.setIfAbsent(eq("idempotency:" + key), any(IdempotencyRecord.class), any(Duration.class)))
                .willReturn(Boolean.FALSE);

        IdempotencyRecord inProgressRecord = IdempotencyRecord.inProgress();
        given(valueOperations.get("idempotency:" + key)).willReturn(inProgressRecord);

        // when
        Object result = idempotencyAspect.execute(joinPoint, idempotent);

        // then
        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("해당 요청이 현재 처리 중입니다. 잠시 후 결과를 다시 확인하십시오.");
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("동일 키가 COMPLETED 상태로 존재할 때 비즈니스 로직 재실행 없이 직전 캐시 응답을 반환한다")
    void execute_WhenCompleted_ShouldReturnCachedResponse() throws Throwable {
        // given
        String key = "test-uuid-completed";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Idempotency-Key", key);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        given(idempotent.headerName()).willReturn("Idempotency-Key");
        given(idempotent.ttl()).willReturn(120L);
        given(idempotent.timeUnit()).willReturn(TimeUnit.SECONDS);

        given(valueOperations.setIfAbsent(eq("idempotency:" + key), any(IdempotencyRecord.class), any(Duration.class)))
                .willReturn(Boolean.FALSE);

        IdempotencyRecord completedRecord = IdempotencyRecord.completed(HttpStatus.OK.value(), Map.of("paymentId", "PAY-999"));
        given(valueOperations.get("idempotency:" + key)).willReturn(completedRecord);

        // when
        Object result = idempotencyAspect.execute(joinPoint, idempotent);

        // then
        assertThat(result).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("paymentId", "PAY-999"));
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("비즈니스 로직 실행 중 예외가 발생하면 Redis 키를 즉시 삭제(롤백)하고 예외를 전파한다")
    void execute_WhenExceptionThrown_ShouldEvictRedisKey() throws Throwable {
        // given
        String key = "test-uuid-exception";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Idempotency-Key", key);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        given(idempotent.headerName()).willReturn("Idempotency-Key");
        given(idempotent.ttl()).willReturn(120L);
        given(idempotent.timeUnit()).willReturn(TimeUnit.SECONDS);

        given(valueOperations.setIfAbsent(eq("idempotency:" + key), any(IdempotencyRecord.class), any(Duration.class)))
                .willReturn(Boolean.TRUE);

        RuntimeException expectedException = new RuntimeException("PG사 결제 통신 실패");
        given(joinPoint.proceed()).willThrow(expectedException);

        // when & then
        assertThatThrownBy(() -> idempotencyAspect.execute(joinPoint, idempotent))
                .isSameAs(expectedException);

        verify(redisTemplate).delete("idempotency:" + key);
    }
}
