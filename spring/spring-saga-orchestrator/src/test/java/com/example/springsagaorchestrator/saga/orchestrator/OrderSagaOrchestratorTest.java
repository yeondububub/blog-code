package com.example.springsagaorchestrator.saga.orchestrator;

import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OrderSagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new OrderSagaOrchestrator(kafkaTemplate);
    }

    @Test
    @DisplayName("SAGA 시작 시 결제 서비스로 결제 명령(payment-commands)을 발행한다")
    void startSaga_ShouldPublishPaymentCommand() {
        // given
        OrderSagaState state = new OrderSagaState("SAGA-001", 1001L, 1L, 2, BigDecimal.valueOf(50000));

        // when
        orchestrator.startSaga(state);

        // then
        verify(kafkaTemplate).send(eq("payment-commands"), eq("SAGA-001"), eq(state));
        assertThat(orchestrator.getSagaState("SAGA-001")).isNotNull();
        assertThat(orchestrator.getSagaState("SAGA-001").getStatus()).isEqualTo(OrderSagaState.SagaStatus.STARTED);
    }

    @Test
    @DisplayName("결제 성공 이벤트 수신 시 다음 단계인 재고 차감 명령(inventory-commands)을 발행한다")
    void handlePaymentEvent_WhenPaymentSuccess_ShouldPublishInventoryCommand() {
        // given
        OrderSagaState state = new OrderSagaState("SAGA-002", 1002L, 1L, 1, BigDecimal.valueOf(30000));
        orchestrator.startSaga(state);

        OrderSagaState event = new OrderSagaState("SAGA-002", 1002L, 1L, 1, BigDecimal.valueOf(30000));
        event.setStatus(OrderSagaState.SagaStatus.PAYMENT_SUCCESS);

        // when
        orchestrator.handlePaymentEvent(event);

        // then
        verify(kafkaTemplate).send(eq("inventory-commands"), eq("SAGA-002"), eq(state));
        assertThat(state.getStatus()).isEqualTo(OrderSagaState.SagaStatus.PAYMENT_SUCCESS);
    }

    @Test
    @DisplayName("결제 실패 이벤트 수신 시 즉시 주문 취소 명령(order-cancel-commands)을 발행한다")
    void handlePaymentEvent_WhenPaymentFailed_ShouldCancelOrder() {
        // given
        OrderSagaState state = new OrderSagaState("SAGA-003", 1003L, 1L, 1, BigDecimal.valueOf(30000));
        orchestrator.startSaga(state);

        OrderSagaState event = new OrderSagaState("SAGA-003", 1003L, 1L, 1, BigDecimal.valueOf(30000));
        event.setStatus(OrderSagaState.SagaStatus.FAILED);
        event.setMessage("한도 초과");

        // when
        orchestrator.handlePaymentEvent(event);

        // then
        verify(kafkaTemplate).send(eq("order-cancel-commands"), eq("SAGA-003"), eq(state));
        assertThat(state.getStatus()).isEqualTo(OrderSagaState.SagaStatus.FAILED);
    }

    @Test
    @DisplayName("재고 차감 성공 이벤트 수신 시 전체 SAGA를 완료(COMPLETED)하고 주문 완료 명령을 발행한다")
    void handleInventoryEvent_WhenInventorySuccess_ShouldCompleteOrder() {
        // given
        OrderSagaState state = new OrderSagaState("SAGA-004", 1004L, 1L, 1, BigDecimal.valueOf(20000));
        orchestrator.startSaga(state);

        OrderSagaState event = new OrderSagaState("SAGA-004", 1004L, 1L, 1, BigDecimal.valueOf(20000));
        event.setStatus(OrderSagaState.SagaStatus.INVENTORY_SUCCESS);

        // when
        orchestrator.handleInventoryEvent(event);

        // then
        verify(kafkaTemplate).send(eq("order-complete-commands"), eq("SAGA-004"), eq(state));
        assertThat(state.getStatus()).isEqualTo(OrderSagaState.SagaStatus.COMPLETED);
    }

    @Test
    @DisplayName("재고 부족 실패 이벤트 수신 시 역순 보상 트랜잭션(결제 취소 및 주문 취소)을 트리거한다")
    void handleInventoryEvent_WhenInventoryFailed_ShouldTriggerCompensatingTransactions() {
        // given
        OrderSagaState state = new OrderSagaState("SAGA-005", 1005L, 999L, 5, BigDecimal.valueOf(70000));
        orchestrator.startSaga(state);

        OrderSagaState event = new OrderSagaState("SAGA-005", 1005L, 999L, 5, BigDecimal.valueOf(70000));
        event.setStatus(OrderSagaState.SagaStatus.FAILED);
        event.setMessage("품절 상품");

        // when
        orchestrator.handleInventoryEvent(event);

        // then
        verify(kafkaTemplate).send(eq("payment-compensate-commands"), eq("SAGA-005"), eq(state));
        verify(kafkaTemplate).send(eq("order-cancel-commands"), eq("SAGA-005"), eq(state));
        assertThat(state.getStatus()).isEqualTo(OrderSagaState.SagaStatus.COMPENSATING);
    }
}
