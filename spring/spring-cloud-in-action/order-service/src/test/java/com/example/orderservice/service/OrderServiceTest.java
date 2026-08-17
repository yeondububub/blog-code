package com.example.orderservice.service;

import com.example.orderservice.client.PaymentClient;
import com.example.orderservice.client.PaymentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("원격 결제 서비스 정상 응답 시 결제 상태를 정상 반환한다")
    void processOrderPayment_Success() {
        // given
        String orderId = "ORD-12345";
        given(paymentClient.getPaymentStatus(orderId))
                .willReturn(new PaymentResponse(orderId, "COMPLETED", "Payment successful"));

        // when
        String status = orderService.processOrderPayment(orderId);

        // then
        assertThat(status).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("원격 결제 서비스 장애 발생 시 fallback 메서드가 실행되어 PAYMENT_PENDING_FALLBACK을 반환한다")
    void fallbackProcessOrderPayment_Triggered() {
        // given
        String orderId = "ORD-99999";
        Throwable error = new RuntimeException("Connection timed out to PAYMENT-SERVICE");

        // when
        String fallbackStatus = orderService.fallbackProcessOrderPayment(orderId, error);

        // then
        assertThat(fallbackStatus).isEqualTo("PAYMENT_PENDING_FALLBACK");
    }
}
