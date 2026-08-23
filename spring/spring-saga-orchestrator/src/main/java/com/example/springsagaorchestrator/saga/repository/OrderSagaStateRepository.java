package com.example.springsagaorchestrator.saga.repository;

import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderSagaStateRepository extends JpaRepository<OrderSagaState, String> {
}
