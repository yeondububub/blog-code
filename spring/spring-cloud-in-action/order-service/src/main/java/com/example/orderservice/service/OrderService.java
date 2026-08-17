package com.example.orderservice.service;

import com.example.orderservice.client.PaymentClient;
import com.example.orderservice.client.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 주문 처리 및 결제 연동 비즈니스 서비스
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final PaymentClient paymentClient;

    public OrderService(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    /**
     * 결제 상태를 검증하고 주문을 처리합니다.
     * 장애율이 임계치를 초과하면 Circuit이 OPEN되어 즉각 fallbackProcessOrderPayment가 실행됩니다.
     */
    @CircuitBreaker(name = "paymentServiceBreaker", fallbackMethod = "fallbackProcessOrderPayment")
    public String processOrderPayment(String orderId) {
        log.info("결제 서비스 원격 호출 시작 - orderId: {}", orderId);
        PaymentResponse response = paymentClient.getPaymentStatus(orderId);
        return response.getStatus();
    }

    /**
     * 원격 결제 서비스 장애 발생 시 실행되는 대체 메서드
     * 원본 메서드와 동일한 반환 타입 및 파라미터 구조를 가져야 하며 Throwable 인자를 추가 수신합니다.
     */
    public String fallbackProcessOrderPayment(String orderId, Throwable throwable) {
        log.warn("결제 서비스 호출 실패 및 서킷 차단 감지 - orderId: {}, cause: {}", orderId, throwable.getMessage());
        // 결제 상태를 대기 상태로 유지하고 비동기 배치 또는 메시지 큐 처리를 위한 상태값을 반환합니다.
        return "PAYMENT_PENDING_FALLBACK";
    }
}
