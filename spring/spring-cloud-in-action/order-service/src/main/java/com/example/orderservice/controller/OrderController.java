package com.example.orderservice.controller;

import com.example.orderservice.config.OrderProperties;
import com.example.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 주문 관리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final OrderProperties orderProperties;

    public OrderController(OrderService orderService, OrderProperties orderProperties) {
        this.orderService = orderService;
        this.orderProperties = orderProperties;
    }

    /**
     * 결제 진행 및 주문 처리 API
     */
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<Map<String, Object>> payOrder(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Gateway-Tracking-Id", required = false) String trackingId) {
        log.info("주문 결제 요청 수신 - orderId: {}, trackingId: {}", orderId, trackingId);
        String paymentStatus = orderService.processOrderPayment(orderId);
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "paymentStatus", paymentStatus,
                "trackingId", trackingId != null ? trackingId : "N/A"
        ));
    }

    /**
     * 단일 주문 조회 API (GET)
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Gateway-Tracking-Id", required = false) String trackingId) {
        log.info("주문 조회 요청 수신 - orderId: {}, trackingId: {}", orderId, trackingId);
        String paymentStatus = orderService.processOrderPayment(orderId);
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "productName", "Spring Cloud In Action 도서",
                "discountRate", orderProperties.getDiscountRate(),
                "paymentStatus", paymentStatus,
                "trackingId", trackingId != null ? trackingId : "N/A"
        ));
    }

    /**
     * 동적 할인율 조회 API (Config Server & @RefreshScope 검증용)
     */
    @GetMapping("/discount")
    public ResponseEntity<Map<String, Object>> getDiscountRate() {
        return ResponseEntity.ok(Map.of(
                "discountRate", orderProperties.getDiscountRate()
        ));
    }
}
