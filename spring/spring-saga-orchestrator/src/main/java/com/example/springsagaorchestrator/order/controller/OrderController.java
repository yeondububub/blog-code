package com.example.springsagaorchestrator.order.controller;

import com.example.springsagaorchestrator.order.dto.OrderCreateRequest;
import com.example.springsagaorchestrator.order.dto.OrderResponse;
import com.example.springsagaorchestrator.order.service.OrderService;
import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import com.example.springsagaorchestrator.saga.orchestrator.OrderSagaOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderSagaOrchestrator sagaOrchestrator;

    public OrderController(OrderService orderService, OrderSagaOrchestrator sagaOrchestrator) {
        this.orderService = orderService;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    /**
     * 주문 생성 및 SAGA 트랜잭션 시작 API
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderCreateRequest request) {
        String sagaId = "SAGA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Long userId = request.getUserId() != null ? request.getUserId() : 1L;
        Long orderId = orderService.createOrder(request.getProductId(), request.getQuantity());

        OrderSagaState sagaState = new OrderSagaState(
                sagaId,
                orderId,
                userId,
                request.getProductId(),
                request.getQuantity(),
                request.getAmount()
        );

        sagaOrchestrator.startSaga(sagaState);

        return ResponseEntity.ok(new OrderResponse(
                sagaId,
                orderId,
                OrderSagaState.SagaStatus.STARTED,
                "주문 접수 및 SAGA 분산 트랜잭션이 시작되었습니다."
        ));
    }

    /**
     * SAGA 트랜잭션 진행 상태 조회 API
     */
    @GetMapping("/saga/{sagaId}")
    public ResponseEntity<OrderSagaState> getSagaState(@PathVariable String sagaId) {
        OrderSagaState state = sagaOrchestrator.getSagaState(sagaId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(state);
    }

    /**
     * 주문 상태 단건 조회 API
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderService.OrderStatus> getOrderStatus(@PathVariable Long orderId) {
        OrderService.OrderStatus status = orderService.getOrderStatus(orderId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }
}
