package com.example.blog.payment;

import com.example.blog.common.idempotency.IdempotencyRecord;
import com.example.blog.payment.dto.PaymentRequest;
import com.example.blog.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentIdempotencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private PaymentService paymentService;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private ValueOperations<String, Object> valueOperations;

    private final Map<String, Object> inMemoryRedis = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        paymentService.resetExecutionCount();
        inMemoryRedis.clear();
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // In-Memory Redis 시뮬레이션
        given(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class)))
                .willAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    Object value = invocation.getArgument(1);
                    if (inMemoryRedis.containsKey(key)) {
                        return false;
                    }
                    inMemoryRedis.put(key, value);
                    return true;
                });

        given(valueOperations.get(anyString()))
                .willAnswer(invocation -> inMemoryRedis.get((String) invocation.getArgument(0)));

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            inMemoryRedis.put(key, value);
            return null;
        }).when(valueOperations).set(anyString(), any(), any(Duration.class));

        given(redisTemplate.delete(anyString()))
                .willAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    return inMemoryRedis.remove(key) != null;
                });
    }

    @Test
    @DisplayName("최초 결제 요청 시 비즈니스 로직이 실행되고 결과가 캐싱된다")
    void firstRequest_ShouldExecutePaymentAndCacheResult() throws Exception {
        String idempotencyKey = "PAY-KEY-1001";
        PaymentRequest request = new PaymentRequest("ORD-1001", new BigDecimal("35000"), "KAKAO_PAY");

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-1001"))
                .andExpect(jsonPath("$.amount").value(35000))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertThat(paymentService.getExecutionCount()).isEqualTo(1);
        assertThat(inMemoryRedis).containsKey("idempotency:" + idempotencyKey);
    }

    @Test
    @DisplayName("동일한 Idempotency-Key로 재요청 시 비즈니스 로직은 재실행되지 않고 직전 응답이 반환된다")
    void duplicateRequest_ShouldReturnCachedResponseWithoutReexecuting() throws Exception {
        String idempotencyKey = "PAY-KEY-2002";
        PaymentRequest request = new PaymentRequest("ORD-2002", new BigDecimal("50000"), "CREDIT_CARD");

        // 1차 요청
        String firstResponseJson = mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(paymentService.getExecutionCount()).isEqualTo(1);

        // 2차 중복 요청 (동일 키)
        String secondResponseJson = mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 비즈니스 로직 실행 횟수는 여전히 1회
        assertThat(paymentService.getExecutionCount()).isEqualTo(1);
        assertThat(secondResponseJson).isEqualTo(firstResponseJson);
    }

    @Test
    @DisplayName("동일 키가 IN_PROGRESS(처리 중)인 상태에서 인입된 중복 요청은 409 CONFLICT를 반환한다")
    void inProgressRequest_ShouldReturn409Conflict() throws Exception {
        String idempotencyKey = "PAY-KEY-3003";
        // 수동으로 IN_PROGRESS 상태 선점 주입
        inMemoryRedis.put("idempotency:" + idempotencyKey, IdempotencyRecord.inProgress());

        PaymentRequest request = new PaymentRequest("ORD-3003", new BigDecimal("10000"), "CARD");

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // 비즈니스 로직은 전혀 호출되지 않아야 함
        assertThat(paymentService.getExecutionCount()).isEqualTo(0);
    }
}
