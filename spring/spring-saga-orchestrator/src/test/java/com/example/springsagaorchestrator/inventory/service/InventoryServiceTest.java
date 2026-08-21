package com.example.springsagaorchestrator.inventory.service;

import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(kafkaTemplate);
    }

    @Test
    @DisplayName("재고가 충분할 경우 차감 후 INVENTORY_SUCCESS 상태로 이벤트를 발행한다")
    void deductInventory_Success() {
        // given
        OrderSagaState command = new OrderSagaState("SAGA-I1", 3001L, 1L, 5, BigDecimal.valueOf(50000));
        int initialStock = inventoryService.getStock(1L);

        // when
        inventoryService.deductInventory(command);

        // then
        assertThat(command.getStatus()).isEqualTo(OrderSagaState.SagaStatus.INVENTORY_SUCCESS);
        assertThat(inventoryService.getStock(1L)).isEqualTo(initialStock - 5);
        verify(kafkaTemplate).send(eq("inventory-events"), eq("SAGA-I1"), eq(command));
    }

    @Test
    @DisplayName("재고가 부족(또는 품절 상품)일 경우 FAILED 상태로 이벤트를 발행한다")
    void deductInventory_OutOfStock() {
        // given (상품 999번은 품절 상품)
        OrderSagaState command = new OrderSagaState("SAGA-I2", 3002L, 999L, 1, BigDecimal.valueOf(10000));

        // when
        inventoryService.deductInventory(command);

        // then
        assertThat(command.getStatus()).isEqualTo(OrderSagaState.SagaStatus.FAILED);
        verify(kafkaTemplate).send(eq("inventory-events"), eq("SAGA-I2"), eq(command));
    }
}
