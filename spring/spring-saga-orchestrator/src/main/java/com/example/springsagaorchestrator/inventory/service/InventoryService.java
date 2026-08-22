package com.example.springsagaorchestrator.inventory.service;

import com.example.springsagaorchestrator.inventory.domain.Inventory;
import com.example.springsagaorchestrator.inventory.repository.InventoryRepository;
import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 재고 서비스: 실제 DB 재고 수량 확인 및 차감 트랜잭션 수행
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final InventoryRepository inventoryRepository;

    public InventoryService(KafkaTemplate<String, Object> kafkaTemplate,
                            InventoryRepository inventoryRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * 재고 차감 로컬 트랜잭션
     */
    @Transactional
    @KafkaListener(topics = "inventory-commands", groupId = "inventory-service-group")
    public void deductInventory(OrderSagaState command) {
        log.info("[재고 서비스] 재고 차감 명령 수신 - sagaId: {}, productId: {}, quantity: {}개",
                command.getSagaId(), command.getProductId(), command.getQuantity());

        try {
            Inventory inventory = inventoryRepository.findByProductId(command.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품 재고 정보를 찾을 수 없습니다. productId: " + command.getProductId()));

            // 재고 수량 차감 비즈니스 로직
            inventory.decreaseStock(command.getQuantity());
            inventoryRepository.save(inventory);

            command.setStatus(OrderSagaState.SagaStatus.INVENTORY_SUCCESS);
            command.setMessage("재고 차감 완료 (남은 재고: " + inventory.getStockQuantity() + "개)");
            log.info("[재고 서비스] 재고 차감 성공 - productId: {}, 남은 재고: {}개",
                    command.getProductId(), inventory.getStockQuantity());

        } catch (Exception ex) {
            command.setStatus(OrderSagaState.SagaStatus.FAILED);
            command.setMessage("재고 차감 실패: " + ex.getMessage());
            log.warn("[재고 서비스] 재고 차감 실패 - productId: {}, 사유: {}", command.getProductId(), ex.getMessage());
        }

        kafkaTemplate.send("inventory-events", command.getSagaId(), command);
    }
}
