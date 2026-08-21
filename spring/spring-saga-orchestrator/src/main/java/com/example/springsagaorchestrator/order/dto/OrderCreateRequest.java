package com.example.springsagaorchestrator.order.dto;

import java.math.BigDecimal;

public class OrderCreateRequest {

    private Long productId;
    private int quantity;
    private BigDecimal amount;

    public OrderCreateRequest() {}

    public OrderCreateRequest(Long productId, int quantity, BigDecimal amount) {
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
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
