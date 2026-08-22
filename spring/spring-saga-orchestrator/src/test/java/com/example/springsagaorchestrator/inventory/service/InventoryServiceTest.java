package com.example.springsagaorchestrator.inventory.service;

import com.example.springsagaorchestrator.inventory.domain.Inventory;
import com.example.springsagaorchestrator.inventory.repository.InventoryRepository;
import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private InventoryRepository inventoryRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(kafkaTemplate, inventoryRepository);
    }

    @Test
    @DisplayName("재고가 충분할 경우 차감 후 INVENTORY_SUCCESS 상태로 이벤트를 발행한다")
    void deductInventory_Success() {
        // given
        Inventory inventory = new Inventory(1L, "맥북 프로", BigDecimal.valueOf(2500000), 10);
        given(inventoryRepository.findByProductId(1L)).willReturn(Optional.of(inventory));

        OrderSagaState command = new OrderSagaState("SAGA-I1", 3001L, 1L, 1L, 2, BigDecimal.valueOf(5000000));

        // when
        inventoryService.deductInventory(command);

        // then
        assertThat(command.getStatus()).isEqualTo(OrderSagaState.SagaStatus.INVENTORY_SUCCESS);
        assertThat(inventory.getStockQuantity()).isEqualTo(8);
        verify(inventoryRepository).save(inventory);
        verify(kafkaTemplate).send(eq("inventory-events"), eq("SAGA-I1"), eq(command));
    }

    @Test
    @DisplayName("재고가 부족할 경우 차감 없이 FAILED 상태로 이벤트를 발행한다")
    void deductInventory_OutOfStock() {
        // given
        Inventory inventory = new Inventory(999L, "품절 상품", BigDecimal.valueOf(10000), 0);
        given(inventoryRepository.findByProductId(999L)).willReturn(Optional.of(inventory));

        OrderSagaState command = new OrderSagaState("SAGA-I2", 3002L, 1L, 999L, 1, BigDecimal.valueOf(10000));

        // when
        inventoryService.deductInventory(command);

        // then
        assertThat(command.getStatus()).isEqualTo(OrderSagaState.SagaStatus.FAILED);
        assertThat(inventory.getStockQuantity()).isEqualTo(0);
        verify(kafkaTemplate).send(eq("inventory-events"), eq("SAGA-I2"), eq(command));
    }
}
