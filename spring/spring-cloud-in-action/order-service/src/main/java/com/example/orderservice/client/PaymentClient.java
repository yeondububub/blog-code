package com.example.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 결제 서비스 호출을 위한 선언적 REST 클라이언트 인터페이스
 */
@FeignClient(
    name = "PAYMENT-SERVICE",         // Eureka에 등록된 대상 서비스 이름
    path = "/api/payments",           // 기본 URI 경로
    fallback = PaymentClientFallback.class // 장애 발생 시 대체 로직을 담당할 빈 클래스 지정
)
public interface PaymentClient {

    /**
     * 주문 ID 기준 결제 상태 조회 API 호출
     */
    @GetMapping("/{orderId}/status")
    PaymentResponse getPaymentStatus(@PathVariable("orderId") String orderId);
}
