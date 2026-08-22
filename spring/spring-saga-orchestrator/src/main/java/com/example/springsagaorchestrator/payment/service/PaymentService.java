package com.example.springsagaorchestrator.payment.service;

import com.example.springsagaorchestrator.account.domain.UserAccount;
import com.example.springsagaorchestrator.account.repository.UserAccountRepository;
import com.example.springsagaorchestrator.payment.domain.PaymentHistory;
import com.example.springsagaorchestrator.payment.repository.PaymentHistoryRepository;
import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 결제 서비스: 실제 사용자 계좌 잔액 차감 및 환불(보상) 트랜잭션 수행
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UserAccountRepository userAccountRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;

    public PaymentService(KafkaTemplate<String, Object> kafkaTemplate,
                          UserAccountRepository userAccountRepository,
                          PaymentHistoryRepository paymentHistoryRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.userAccountRepository = userAccountRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
    }

    /**
     * 1. 정상 결제 로컬 트랜잭션 (계좌 잔액 차감 및 결제 이력 저장)
     */
    @Transactional
    @KafkaListener(topics = "payment-commands", groupId = "payment-service-group")
    public void processPayment(OrderSagaState command) {
        log.info("[결제 서비스] 결제 명령 수신 - sagaId: {}, userId: {}, orderId: {}, amount: {}원",
                command.getSagaId(), command.getUserId(), command.getOrderId(), command.getAmount());

        try {
            UserAccount account = userAccountRepository.findByUserId(command.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("사용자 계좌를 찾을 수 없습니다. userId: " + command.getUserId()));

            // 계좌 잔액 차감 비즈니스 로직
            account.debit(command.getAmount());
            userAccountRepository.save(account);

            // 결제 이력 저장
            PaymentHistory paymentHistory = new PaymentHistory(command.getOrderId(), command.getUserId(), command.getAmount());
            paymentHistoryRepository.save(paymentHistory);

            command.setStatus(OrderSagaState.SagaStatus.PAYMENT_SUCCESS);
            command.setMessage("계좌 잔액 출금 및 결제 승인 완료 (남은 잔액: " + account.getBalance() + "원)");
            log.info("[결제 서비스] 결제 성공 - userId: {}, 남은 잔액: {}원", command.getUserId(), account.getBalance());

        } catch (Exception ex) {
            command.setStatus(OrderSagaState.SagaStatus.FAILED);
            command.setMessage("결제 승인 실패: " + ex.getMessage());
            log.warn("[결제 서비스] 결제 실패 처리 - orderId: {}, 사유: {}", command.getOrderId(), ex.getMessage());
        }

        // 결과를 오케스트레이터 응답 토픽으로 전송
        kafkaTemplate.send("payment-events", command.getSagaId(), command);
    }

    /**
     * 2. 결제 취소 보상 트랜잭션 (Compensating Transaction: 계좌 잔액 환불 원복)
     */
    @Transactional
    @KafkaListener(topics = "payment-compensate-commands", groupId = "payment-service-group")
    public void compensatePayment(OrderSagaState command) {
        log.warn("[결제 서비스] 보상 트랜잭션 실행(계좌 환불 원복) - sagaId: {}, orderId: {}, amount: {}원",
                command.getSagaId(), command.getOrderId(), command.getAmount());

        Optional<PaymentHistory> historyOpt = paymentHistoryRepository.findByOrderId(command.getOrderId());
        if (historyOpt.isPresent()) {
            PaymentHistory history = historyOpt.get();
            if (history.getStatus() == PaymentHistory.PaymentStatus.APPROVED) {
                // 사용자 계좌로 환불 입금
                userAccountRepository.findByUserId(history.getUserId()).ifPresent(account -> {
                    account.credit(history.getAmount());
                    userAccountRepository.save(account);
                    log.info("[결제 서비스] 계좌 환불 입금 완료 - userId: {}, 복구된 잔액: {}원",
                            account.getUserId(), account.getBalance());
                });

                // 결제 이력 상태 REFUNDED로 변경
                history.refund();
                paymentHistoryRepository.save(history);
            }
        }
    }
}
