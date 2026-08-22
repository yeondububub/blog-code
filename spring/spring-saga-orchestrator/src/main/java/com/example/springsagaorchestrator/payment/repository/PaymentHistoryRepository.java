package com.example.springsagaorchestrator.payment.repository;

import com.example.springsagaorchestrator.payment.domain.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    Optional<PaymentHistory> findByOrderId(Long orderId);
}
