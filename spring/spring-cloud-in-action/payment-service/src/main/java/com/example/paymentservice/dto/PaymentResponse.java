package com.example.paymentservice.dto;

/**
 * 결제 상태 응답 DTO
 */
public class PaymentResponse {

    private String orderId;
    private String status;
    private String message;

    public PaymentResponse() {
    }

    public PaymentResponse(String orderId, String status, String message) {
        this.orderId = orderId;
        this.status = status;
        this.message = message;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
