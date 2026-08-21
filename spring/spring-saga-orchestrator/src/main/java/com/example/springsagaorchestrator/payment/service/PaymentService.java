package com.example.springsagaorchestrator.payment.service;

import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 결제 서비스 및 보상 트랜잭션 리스너
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 결제 완료 내역 저장소 (보상 트랜잭션 시 환불 대상 식별용)
    private final Set<Long> processedPayments = ConcurrentHashMap.newKeySet();

    public PaymentService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 정상 결제 처리 로컬 트랜잭션
     */
    @KafkaListener(topics = "payment-commands", groupId = "payment-service-group")
    public void processPayment(OrderSagaState command) {
        log.info("[결제 서비스] 결제 명령 수신 - sagaId: {}, orderId: {}, amount: {}",
                command.getSagaId(), command.getOrderId(), command.getAmount());

        try {
            boolean paymentSuccess = executePayment(command.getOrderId(), command.getAmount());

            if (paymentSuccess) {
                processedPayments.add(command.getOrderId());
                command.setStatus(OrderSagaState.SagaStatus.PAYMENT_SUCCESS);
                command.setMessage("결제 승인 완료");
                log.info("[결제 서비스] 결제 성공 처리 - orderId: {}", command.getOrderId());
            } else {
                command.setStatus(OrderSagaState.SagaStatus.FAILED);
                command.setMessage("결제 승인 실패 (한도 초과 또는 잔액 부족)");
                log.warn("[결제 서비스] 결제 실패 처리 - orderId: {}", command.getOrderId());
            }
        } catch (Exception ex) {
            command.setStatus(OrderSagaState.SagaStatus.FAILED);
            command.setMessage("결제 처리 중 예외 발생: " + ex.getMessage());
            log.error("[결제 서비스] 결제 처리 오류 - orderId: {}", command.getOrderId(), ex);
        }

        // 결과를 오케스트레이터 응답 토픽으로 전송합니다.
        kafkaTemplate.send("payment-events", command.getSagaId(), command);
    }

    /**
     * 결제 취소 보상 트랜잭션 (Compensating Transaction)
     */
    @KafkaListener(topics = "payment-compensate-commands", groupId = "payment-service-group")
    public void compensatePayment(OrderSagaState command) {
        log.warn("[결제 서비스] 보상 트랜잭션(결제 취소/환불) 실행 - sagaId: {}, orderId: {}, amount: {}",
                command.getSagaId(), command.getOrderId(), command.getAmount());

        refundPayment(command.getOrderId(), command.getAmount());
    }

    private boolean executePayment(Long orderId, BigDecimal amount) {
        // 금액이 1,000,000원을 초과하거나 0 이하일 경우 결제 실패 시뮬레이션
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
            return false;
        }
        return true;
    }

    private void refundPayment(Long orderId, BigDecimal amount) {
        processedPayments.remove(orderId);
        log.info("[결제 서비스] 결제 환불 완료 - orderId: {}, amount: {}", orderId, amount);
    }

    public boolean isPaymentProcessed(Long orderId) {
        return processedPayments.contains(orderId);
    }
}
