package com.example.springsagaorchestrator.saga.orchestrator;

import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 주문 분산 트랜잭션의 상태 머신을 관리하는 SAGA 오케스트레이터
 */
@Component
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    // 실무 환경에서는 Redis나 RDB에 Saga 상태를 영속화하여 관리합니다.
    private final Map<String, OrderSagaState> sagaRepository = new ConcurrentHashMap<>();

    public OrderSagaOrchestrator(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * SAGA 시작점: 결제 명령 발행
     */
    public void startSaga(OrderSagaState sagaState) {
        sagaRepository.put(sagaState.getSagaId(), sagaState);
        log.info("[SAGA 오케스트레이터] SAGA 트랜잭션 시작 - sagaId: {}, orderId: {}",
                sagaState.getSagaId(), sagaState.getOrderId());

        // 1. 결제 서비스로 결제 명령(Command)을 발행합니다.
        kafkaTemplate.send("payment-commands", sagaState.getSagaId(), sagaState);
    }

    /**
     * 결제 처리 결과 수신
     */
    @KafkaListener(topics = "payment-events", groupId = "saga-orchestrator-group")
    public void handlePaymentEvent(OrderSagaState event) {
        OrderSagaState state = sagaRepository.get(event.getSagaId());
        if (state == null) {
            log.warn("[SAGA 오케스트레이터] 유효하지 않은 sagaId: {}", event.getSagaId());
            return;
        }

        log.info("[SAGA 오케스트레이터] 결제 이벤트 수신 - sagaId: {}, status: {}", event.getSagaId(), event.getStatus());

        if (event.getStatus() == OrderSagaState.SagaStatus.PAYMENT_SUCCESS) {
            state.setStatus(OrderSagaState.SagaStatus.PAYMENT_SUCCESS);
            state.setMessage(event.getMessage());
            // 2. 결제 성공 시 다음 단계인 재고 차감 명령을 발행합니다.
            log.info("[SAGA 오케스트레이터] 재고 차감 명령 발행 (inventory-commands)");
            kafkaTemplate.send("inventory-commands", state.getSagaId(), state);
        } else {
            // 결제 실패 시 즉시 주문 취소 보상 트랜잭션을 실행합니다.
            state.setStatus(OrderSagaState.SagaStatus.FAILED);
            state.setMessage(event.getMessage());
            log.warn("[SAGA 오케스트레이터]  결제 실패 감지 -> 주문 취소 명령 발행 (order-cancel-commands)");
            kafkaTemplate.send("order-cancel-commands", state.getSagaId(), state);
        }
    }

    /**
     * 재고 처리 결과 수신 및 보상 트랜잭션 조율
     */
    @KafkaListener(topics = "inventory-events", groupId = "saga-orchestrator-group")
    public void handleInventoryEvent(OrderSagaState event) {
        OrderSagaState state = sagaRepository.get(event.getSagaId());
        if (state == null) {
            log.warn("[SAGA 오케스트레이터] 유효하지 않은 sagaId: {}", event.getSagaId());
            return;
        }

        log.info("[SAGA 오케스트레이터] 재고 이벤트 수신 - sagaId: {}, status: {}", event.getSagaId(), event.getStatus());

        if (event.getStatus() == OrderSagaState.SagaStatus.INVENTORY_SUCCESS) {
            // 3. 재고 차감까지 성공하면 전체 SAGA 트랜잭션을 완료 처리합니다.
            state.setStatus(OrderSagaState.SagaStatus.COMPLETED);
            state.setMessage("주문 및 분산 트랜잭션 정상 완료");
            log.info("[SAGA 오케스트레이터] 전체 SAGA 성공 완료 -> 주문 완료 명령 발행 (order-complete-commands)");
            kafkaTemplate.send("order-complete-commands", state.getSagaId(), state);
        } else {
            // 4. 재고 차감 실패 시 역순으로 결제 환불 및 주문 취소 보상 트랜잭션을 트리거합니다.
            state.setStatus(OrderSagaState.SagaStatus.COMPENSATING);
            state.setMessage("재고 부족으로 인한 롤백 및 보상 트랜잭션 실행: " + event.getMessage());
            log.warn("[SAGA 오케스트레이터] 재고 부족 감지 -> 결제 취소 및 주문 취소 역순 보상 트랜잭션 트리거!");
            kafkaTemplate.send("payment-compensate-commands", state.getSagaId(), state);
            kafkaTemplate.send("order-cancel-commands", state.getSagaId(), state);
        }
    }

    public OrderSagaState getSagaState(String sagaId) {
        return sagaRepository.get(sagaId);
    }
}
