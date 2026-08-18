package com.example.blog.payment.service;

import com.example.blog.payment.dto.PaymentRequest;
import com.example.blog.payment.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final AtomicInteger executionCount = new AtomicInteger(0);

    /**
     * 실제 결제 처리 비즈니스 로직
     */
    public PaymentResponse charge(PaymentRequest request) {
        int count = executionCount.incrementAndGet();
        log.info("[결제 처리 실행 #{} ] orderId: {}, amount: {}, method: {}",
                count, request.getOrderId(), request.getAmount(), request.getPaymentMethod());

        // 결제 승인 ID 생성 및 응답 반환
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentResponse(
                paymentId,
                request.getOrderId(),
                request.getAmount(),
                "APPROVED",
                LocalDateTime.now()
        );
    }

    public int getExecutionCount() {
        return executionCount.get();
    }

    public void resetExecutionCount() {
        executionCount.set(0);
    }
}
