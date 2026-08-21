package com.example.springsagaorchestrator.order.service;

import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 주문 도메인 서비스 및 최종 주문 상태 갱신 리스너
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final AtomicLong orderIdGenerator = new AtomicLong(1000);

    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        CANCELLED
    }

    private final Map<Long, OrderStatus> orderRepository = new ConcurrentHashMap<>();

    public Long createOrder(Long productId, int quantity) {
        Long orderId = orderIdGenerator.incrementAndGet();
        orderRepository.put(orderId, OrderStatus.PENDING);
        log.info("[주문 서비스] 신규 주문 생성 (PENDING) - orderId: {}, productId: {}, quantity: {}",
                orderId, productId, quantity);
        return orderId;
    }

    @KafkaListener(topics = "order-complete-commands", groupId = "order-service-group")
    public void completeOrder(OrderSagaState command) {
        orderRepository.put(command.getOrderId(), OrderStatus.CONFIRMED);
        log.info("[주문 서비스] 주문 최종 체결 완료 (CONFIRMED) - orderId: {}, sagaId: {}",
                command.getOrderId(), command.getSagaId());
    }

    @KafkaListener(topics = "order-cancel-commands", groupId = "order-service-group")
    public void cancelOrder(OrderSagaState command) {
        orderRepository.put(command.getOrderId(), OrderStatus.CANCELLED);
        log.warn("[주문 서비스] 주문 취소/롤백 완료 (CANCELLED) - orderId: {}, sagaId: {}, 이유: {}",
                command.getOrderId(), command.getSagaId(), command.getMessage());
    }

    public OrderStatus getOrderStatus(Long orderId) {
        return orderRepository.get(orderId);
    }
}
