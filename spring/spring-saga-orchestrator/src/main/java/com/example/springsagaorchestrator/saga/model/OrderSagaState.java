package com.example.springsagaorchestrator.saga.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 주문 분산 트랜잭션의 상태 머신 모델
 */
public class OrderSagaState implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum SagaStatus {
        STARTED,
        PAYMENT_SUCCESS,
        INVENTORY_SUCCESS,
        COMPENSATING,
        FAILED,
        COMPLETED
    }

    private String sagaId;
    private Long orderId;
    private Long userId;
    private Long productId;
    private int quantity;
    private BigDecimal amount;
    private SagaStatus status;
    private String message;

    public OrderSagaState() {}

    public OrderSagaState(String sagaId, Long orderId, Long productId, int quantity, BigDecimal amount) {
        this(sagaId, orderId, 1L, productId, quantity, amount);
    }

    public OrderSagaState(String sagaId, Long orderId, Long userId, Long productId, int quantity, BigDecimal amount) {
        this.sagaId = sagaId;
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
        this.status = SagaStatus.STARTED;
    }

    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public SagaStatus getStatus() { return status; }
    public void setStatus(SagaStatus status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
