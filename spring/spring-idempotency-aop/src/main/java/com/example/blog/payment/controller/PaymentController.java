package com.example.blog.payment.controller;

import com.example.blog.common.idempotency.Idempotent;
import com.example.blog.payment.dto.PaymentRequest;
import com.example.blog.payment.dto.PaymentResponse;
import com.example.blog.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Idempotent(headerName = "Idempotency-Key", ttl = 300) // 5분 동안 동일 키 중복 요청 차단 및 결과 캐싱
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody PaymentRequest request) {

        PaymentResponse response = paymentService.charge(request);
        return ResponseEntity.ok(response);
    }
}
