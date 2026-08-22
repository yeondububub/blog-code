package com.example.springsagaorchestrator.order.controller;

import com.example.springsagaorchestrator.order.dto.OrderCreateRequest;
import com.example.springsagaorchestrator.order.service.OrderService;
import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import com.example.springsagaorchestrator.saga.orchestrator.OrderSagaOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderSagaOrchestrator sagaOrchestrator;

    @Test
    @DisplayName("주문 생성 API 호출 시 200 OK와 함께 SAGA 시작 상태가 반환된다")
    void createOrder_Success() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(1L, 2, BigDecimal.valueOf(50000));
        given(orderService.createOrder(1L, 2)).willReturn(1001L);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1001))
                .andExpect(jsonPath("$.sagaId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("STARTED"));
    }

    @Test
    @DisplayName("SAGA 상태 조회 API 호출 시 현재 진행 상태를 반환한다")
    void getSagaState_Success() throws Exception {
        OrderSagaState state = new OrderSagaState("SAGA-100", 1001L, 1L, 2, BigDecimal.valueOf(50000));
        state.setStatus(OrderSagaState.SagaStatus.COMPLETED);
        state.setMessage("주문 및 분산 트랜잭션 정상 완료");

        given(sagaOrchestrator.getSagaState("SAGA-100")).willReturn(state);

        mockMvc.perform(get("/api/v1/orders/saga/SAGA-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sagaId").value("SAGA-100"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
