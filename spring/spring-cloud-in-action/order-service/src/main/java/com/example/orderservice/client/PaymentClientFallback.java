package com.example.orderservice.client;

import org.springframework.stereotype.Component;

/**
 * Feign 클라이언트 통신 실패 시 동작하는 Fallback 구현체
 */
@Component
public class PaymentClientFallback implements PaymentClient {

    @Override
    public PaymentResponse getPaymentStatus(String orderId) {
        return new PaymentResponse(
                orderId,
                "PAYMENT_PENDING_FALLBACK",
                "결제 서비스 통신 실패로 인해 Fallback 응답이 반환되었습니다."
        );
    }
}
