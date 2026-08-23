package com.example.springsagaorchestrator.saga.orchestrator;

import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import com.example.springsagaorchestrator.saga.repository.OrderSagaStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * RDB에 SAGA 상태를 영속화하여 관리하는 중앙 오케스트레이터
 */
@Component
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderSagaStateRepository sagaStateRepository;

    public OrderSagaOrchestrator(KafkaTemplate<String, Object> kafkaTemplate,
                                 OrderSagaStateRepository sagaStateRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.sagaStateRepository = sagaStateRepository;
    }

    /**
     * SAGA 시작점: RDB 상태 저장 후 결제 명령 발행
     */
    @Transactional
    public void startSaga(OrderSagaState sagaState) {
        // 1. SAGA 상태를 RDB에 STARTED로 영속화
        sagaStateRepository.save(sagaState);
        log.info("[SAGA 오케스트레이터] SAGA 상태 RDB 저장 및 시작 - sagaId: {}, orderId: {}",
                sagaState.getSagaId(), sagaState.getOrderId());

        // 2. 결제 서비스로 결제 명령(Command) 발행
        kafkaTemplate.send("payment-commands", sagaState.getSagaId(), sagaState);
    }

    /**
     * 결제 처리 결과 수신
     */
    @Transactional
    @KafkaListener(topics = "payment-events", groupId = "saga-orchestrator-group")
    public void handlePaymentEvent(OrderSagaState event) {
        OrderSagaState state = sagaStateRepository.findById(event.getSagaId()).orElse(null);
        if (state == null) {
            log.warn("[SAGA 오케스트레이터] 유효하지 않은 sagaId: {}", event.getSagaId());
            return;
        }

        log.info("[SAGA 오케스트레이터] 결제 이벤트 수신 - sagaId: {}, status: {}", event.getSagaId(), event.getStatus());

        if (event.getStatus() == OrderSagaState.SagaStatus.PAYMENT_SUCCESS) {
            state.updateStatus(OrderSagaState.SagaStatus.PAYMENT_SUCCESS, event.getMessage());
            sagaStateRepository.save(state);

            // 결제 성공 시 재고 차감 명령 발행
            log.info("[SAGA 오케스트레이터] 재고 차감 명령 발행 (inventory-commands)");
            kafkaTemplate.send("inventory-commands", state.getSagaId(), state);
        } else {
            state.updateStatus(OrderSagaState.SagaStatus.FAILED, event.getMessage());
            sagaStateRepository.save(state);

            // 결제 실패 시 즉시 주문 취소 명령 발행
            log.warn("[SAGA 오케스트레이터] 결제 실패 감지 -> 주문 취소 명령 발행 (order-cancel-commands)");
            kafkaTemplate.send("order-cancel-commands", state.getSagaId(), state);
        }
    }

    /**
     * 재고 처리 결과 수신 및 보상 트랜잭션 조율
     */
    @Transactional
    @KafkaListener(topics = "inventory-events", groupId = "saga-orchestrator-group")
    public void handleInventoryEvent(OrderSagaState event) {
        OrderSagaState state = sagaStateRepository.findById(event.getSagaId()).orElse(null);
        if (state == null) {
            log.warn("[SAGA 오케스트레이터] 유효하지 않은 sagaId: {}", event.getSagaId());
            return;
        }

        log.info("[SAGA 오케스트레이터] 재고 이벤트 수신 - sagaId: {}, status: {}", event.getSagaId(), event.getStatus());

        if (event.getStatus() == OrderSagaState.SagaStatus.INVENTORY_SUCCESS) {
            state.updateStatus(OrderSagaState.SagaStatus.COMPLETED, "주문 및 분산 트랜잭션 정상 완료");
            sagaStateRepository.save(state);

            // 최종 성공: 주문 완료 명령 발행
            log.info("[SAGA 오케스트레이터] 전체 SAGA 성공 완료 -> 주문 완료 명령 발행 (order-complete-commands)");
            kafkaTemplate.send("order-complete-commands", state.getSagaId(), state);
        } else {
            state.updateStatus(OrderSagaState.SagaStatus.COMPENSATING, "재고 부족으로 인한 롤백: " + event.getMessage());
            sagaStateRepository.save(state);

            // 실패: 역순 보상 트랜잭션 트리거
            log.warn("[SAGA 오케스트레이터] 재고 부족 감지 -> 결제 취소 및 주문 취소 역순 보상 트랜잭션 트리거");
            kafkaTemplate.send("payment-compensate-commands", state.getSagaId(), state);
            kafkaTemplate.send("order-cancel-commands", state.getSagaId(), state);
        }
    }

    @Transactional(readOnly = true)
    public OrderSagaState getSagaState(String sagaId) {
        return sagaStateRepository.findById(sagaId).orElse(null);
    }
}
