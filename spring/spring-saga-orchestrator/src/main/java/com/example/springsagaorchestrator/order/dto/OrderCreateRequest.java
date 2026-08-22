package com.example.springsagaorchestrator.order.dto;

import java.math.BigDecimal;

public class OrderCreateRequest {

    private Long userId;
    private Long productId;
    private int quantity;
    private BigDecimal amount;

    public OrderCreateRequest() {}

    public OrderCreateRequest(Long productId, int quantity, BigDecimal amount) {
        this(1L, productId, quantity, amount);
    }

    public OrderCreateRequest(Long userId, Long productId, int quantity, BigDecimal amount) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
