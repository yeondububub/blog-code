package com.example.springsagaorchestrator.order.dto;

import com.example.springsagaorchestrator.saga.model.OrderSagaState;

public class OrderResponse {

    private String sagaId;
    private Long orderId;
    private OrderSagaState.SagaStatus status;
    private String message;

    public OrderResponse() {}

    public OrderResponse(String sagaId, Long orderId, OrderSagaState.SagaStatus status, String message) {
        this.sagaId = sagaId;
        this.orderId = orderId;
        this.status = status;
        this.message = message;
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public OrderSagaState.SagaStatus getStatus() {
        return status;
    }

    public void setStatus(OrderSagaState.SagaStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
