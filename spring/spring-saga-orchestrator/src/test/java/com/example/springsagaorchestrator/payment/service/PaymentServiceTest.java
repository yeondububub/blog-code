package com.example.springsagaorchestrator.payment.service;

import com.example.springsagaorchestrator.account.domain.UserAccount;
import com.example.springsagaorchestrator.account.repository.UserAccountRepository;
import com.example.springsagaorchestrator.payment.domain.PaymentHistory;
import com.example.springsagaorchestrator.payment.repository.PaymentHistoryRepository;
import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(kafkaTemplate, userAccountRepository, paymentHistoryRepository);
    }

    @Test
    @DisplayName("계좌 잔액이 충분할 경우 잔액 차감 후 PAYMENT_SUCCESS 상태로 이벤트를 발행한다")
    void processPayment_Success() {
        // given
        UserAccount account = new UserAccount(1L, "110-123-456789", BigDecimal.valueOf(100000));
        given(userAccountRepository.findByUserId(1L)).willReturn(Optional.of(account));

        OrderSagaState command = new OrderSagaState("SAGA-P1", 2001L, 1L, 1L, 1, BigDecimal.valueOf(30000));

        // when
        paymentService.processPayment(command);

        // then
        assertThat(command.getStatus()).isEqualTo(OrderSagaState.SagaStatus.PAYMENT_SUCCESS);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        verify(userAccountRepository).save(account);
        verify(paymentHistoryRepository).save(any(PaymentHistory.class));
        verify(kafkaTemplate).send(eq("payment-events"), eq("SAGA-P1"), eq(command));
    }

    @Test
    @DisplayName("계좌 잔액이 부족할 경우 차감 없이 FAILED 상태로 이벤트를 발행한다")
    void processPayment_InsufficientBalance() {
        // given
        UserAccount account = new UserAccount(2L, "110-999-888888", BigDecimal.valueOf(5000));
        given(userAccountRepository.findByUserId(2L)).willReturn(Optional.of(account));

        OrderSagaState command = new OrderSagaState("SAGA-P2", 2002L, 2L, 1L, 1, BigDecimal.valueOf(50000));

        // when
        paymentService.processPayment(command);

        // then
        assertThat(command.getStatus()).isEqualTo(OrderSagaState.SagaStatus.FAILED);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        verify(kafkaTemplate).send(eq("payment-events"), eq("SAGA-P2"), eq(command));
    }

    @Test
    @DisplayName("결제 취소 보상 트랜잭션 수신 시 사용자 계좌로 환불 입금하고 결제 상태를 REFUNDED로 변경한다")
    void compensatePayment_RefundSuccess() {
        // given
        PaymentHistory history = new PaymentHistory(2003L, 1L, BigDecimal.valueOf(30000));
        UserAccount account = new UserAccount(1L, "110-123-456789", BigDecimal.valueOf(70000));

        given(paymentHistoryRepository.findByOrderId(2003L)).willReturn(Optional.of(history));
        given(userAccountRepository.findByUserId(1L)).willReturn(Optional.of(account));

        OrderSagaState command = new OrderSagaState("SAGA-P3", 2003L, 1L, 1L, 1, BigDecimal.valueOf(30000));

        // when
        paymentService.compensatePayment(command);

        // then
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        assertThat(history.getStatus()).isEqualTo(PaymentHistory.PaymentStatus.REFUNDED);
        verify(userAccountRepository).save(account);
        verify(paymentHistoryRepository).save(history);
    }
}
