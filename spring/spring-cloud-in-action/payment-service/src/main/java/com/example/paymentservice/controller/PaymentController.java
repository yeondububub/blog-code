package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 서비스 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    /**
     * 주문 ID 기준 결제 상태 조회 API
     *
     * @param orderId 주문 식별자
     * @param simulateError 서킷 브레이커 테스트를 위한 고의적 장애 발생 여부 (옵션)
     * @param delayMs 서킷 브레이커 지연(slow-call) 테스트를 위한 지연 시간 ms (옵션)
     * @return 결제 응답 DTO
     */
    @GetMapping("/{orderId}/status")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "false") boolean simulateError,
            @RequestParam(defaultValue = "0") long delayMs) {

        log.info("결제 상태 조회 요청 수신 - orderId: {}, simulateError: {}, delayMs: {}", orderId, simulateError, delayMs);

        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (simulateError) {
            log.error("테스트용 고의적 결제 서비스 오류 발생 - orderId: {}", orderId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PaymentResponse(orderId, "FAILED", "Payment service internal error"));
        }

        return ResponseEntity.ok(new PaymentResponse(orderId, "COMPLETED", "결제가 정상적으로 완료되었습니다."));
    }
}
