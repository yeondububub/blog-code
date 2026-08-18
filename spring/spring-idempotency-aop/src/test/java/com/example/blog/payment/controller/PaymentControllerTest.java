package com.example.blog.payment.controller;

import com.example.blog.payment.dto.PaymentRequest;
import com.example.blog.payment.dto.PaymentResponse;
import com.example.blog.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("결제 요청 API 호출 시 정상 응답을 반환한다")
    void processPayment_Success() throws Exception {
        // given
        PaymentRequest request = new PaymentRequest("ORD-1001", new BigDecimal("50000"), "CREDIT_CARD");
        PaymentResponse response = new PaymentResponse("PAY-12345678", "ORD-1001", new BigDecimal("50000"), "APPROVED", LocalDateTime.now());

        given(paymentService.charge(any(PaymentRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("PAY-12345678"))
                .andExpect(jsonPath("$.orderId").value("ORD-1001"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
