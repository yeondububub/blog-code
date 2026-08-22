package com.example.springsagaorchestrator.order.repository;

import com.example.springsagaorchestrator.order.domain.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findBySagaId(String sagaId);
}
