package com.example.springsagaorchestrator.payment.service;

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
class PaymentServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(kafkaTemplate);
    }

    @Test
    @DisplayName("유효한 금액의 결제 명령 수신 시 PAYMENT_SUCCESS 상태로 이벤트를 발행한다")
    void processPayment_Success() {
        // given
        OrderSagaState command = new OrderSagaState("SAGA-P1", 2001L, 1L, 1, BigDecimal.valueOf(50000));

        // when
        paymentService.processPayment(command);

        // then
        assertThat(command.getStatus()).isEqualTo(OrderSagaState.SagaStatus.PAYMENT_SUCCESS);
        assertThat(paymentService.isPaymentProcessed(2001L)).isTrue();
        verify(kafkaTemplate).send(eq("payment-events"), eq("SAGA-P1"), eq(command));
    }

    @Test
    @DisplayName("비정상 금액(초과/음수)의 결제 명령 수신 시 FAILED 상태로 이벤트를 발행한다")
    void processPayment_Failed() {
        // given
        OrderSagaState command = new OrderSagaState("SAGA-P2", 2002L, 1L, 1, BigDecimal.valueOf(2_000_000));

        // when
        paymentService.processPayment(command);

        // then
        assertThat(command.getStatus()).isEqualTo(OrderSagaState.SagaStatus.FAILED);
        assertThat(paymentService.isPaymentProcessed(2002L)).isFalse();
        verify(kafkaTemplate).send(eq("payment-events"), eq("SAGA-P2"), eq(command));
    }

    @Test
    @DisplayName("결제 취소 보상 트랜잭션 수신 시 환불 처리를 수행한다")
    void compensatePayment() {
        // given
        OrderSagaState command = new OrderSagaState("SAGA-P3", 2003L, 1L, 1, BigDecimal.valueOf(50000));
        paymentService.processPayment(command);
        assertThat(paymentService.isPaymentProcessed(2003L)).isTrue();

        // when
        paymentService.compensatePayment(command);

        // then
        assertThat(paymentService.isPaymentProcessed(2003L)).isFalse();
    }
}
