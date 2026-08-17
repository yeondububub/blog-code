package com.example.orderservice.controller;

import com.example.orderservice.config.OrderProperties;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderProperties orderProperties;

    @Test
    @DisplayName("주문 결제 API 호출 시 정상 응답 반환")
    void payOrder_Success() throws Exception {
        given(orderService.processOrderPayment("ORD-100")).willReturn("COMPLETED");

        mockMvc.perform(post("/api/orders/ORD-100/pay")
                        .header("X-Gateway-Tracking-Id", "test-tracking-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-100"))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.trackingId").value("test-tracking-id"));
    }

    @Test
    @DisplayName("할인율 조회 API 호출 시 프로퍼티 값 반환")
    void getDiscountRate_Success() throws Exception {
        given(orderProperties.getDiscountRate()).willReturn(15.0);

        mockMvc.perform(get("/api/orders/discount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountRate").value(15.0));
    }
}
