package com.example.springsagaorchestrator.order.service;

import com.example.springsagaorchestrator.order.domain.OrderEntity;
import com.example.springsagaorchestrator.order.repository.OrderRepository;
import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 주문 서비스: 실제 DB 주문 레코드 생성 및 최종 상태 갱신
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * 신규 주문 생성 (PENDING 상태로 저장)
     */
    @Transactional
    public Long createOrder(String sagaId, Long userId, Long productId, int quantity, BigDecimal amount) {
        OrderEntity order = new OrderEntity(sagaId, userId, productId, quantity, amount);
        OrderEntity savedOrder = orderRepository.save(order);
        log.info("[주문 서비스] 신규 주문 생성 (PENDING) - orderId: {}, sagaId: {}, userId: {}",
                savedOrder.getId(), sagaId, userId);
        return savedOrder.getId();
    }

    /**
     * SAGA 성공 완료 시 주문 상태를 CONFIRMED로 변경
     */
    @Transactional
    @KafkaListener(topics = "order-complete-commands", groupId = "order-service-group")
    public void completeOrder(OrderSagaState command) {
        Optional<OrderEntity> orderOpt = orderRepository.findById(command.getOrderId());
        orderOpt.ifPresent(order -> {
            order.complete();
            orderRepository.save(order);
            log.info("[주문 서비스] 🎉 주문 최종 체결 완료 (CONFIRMED) - orderId: {}, sagaId: {}",
                    order.getId(), command.getSagaId());
        });
    }

    /**
     * SAGA 실패 시 주문 상태를 CANCELLED로 변경 (보상 트랜잭션)
     */
    @Transactional
    @KafkaListener(topics = "order-cancel-commands", groupId = "order-service-group")
    public void cancelOrder(OrderSagaState command) {
        Optional<OrderEntity> orderOpt = orderRepository.findById(command.getOrderId());
        orderOpt.ifPresent(order -> {
            order.cancel();
            orderRepository.save(order);
            log.warn("[주문 서비스] ❌ 주문 취소/롤백 완료 (CANCELLED) - orderId: {}, sagaId: {}, 이유: {}",
                    order.getId(), command.getSagaId(), command.getMessage());
        });
    }

    @Transactional(readOnly = true)
    public OrderEntity.OrderStatus getOrderStatus(Long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderEntity::getStatus)
                .orElse(null);
    }
}
