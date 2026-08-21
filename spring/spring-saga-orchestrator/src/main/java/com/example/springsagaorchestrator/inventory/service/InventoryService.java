package com.example.springsagaorchestrator.inventory.service;

import com.example.springsagaorchestrator.saga.model.OrderSagaState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 재고 서비스 및 로컬 트랜잭션
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 상품별 기본 재고 수량
    private final Map<Long, Integer> stockRepository = new ConcurrentHashMap<>();

    public InventoryService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        // 기본 상품 재고 초기화 (상품 1번: 100개, 상품 2번: 5개, 상품 999번: 0개 품절 상품)
        stockRepository.put(1L, 100);
        stockRepository.put(2L, 5);
        stockRepository.put(999L, 0);
    }

    /**
     * 재고 차감 로컬 트랜잭션
     */
    @KafkaListener(topics = "inventory-commands", groupId = "inventory-service-group")
    public void deductInventory(OrderSagaState command) {
        log.info("[재고 서비스] 재고 차감 명령 수신 - sagaId: {}, productId: {}, quantity: {}",
                command.getSagaId(), command.getProductId(), command.getQuantity());

        boolean stockDeducted = executeDeduct(command.getProductId(), command.getQuantity());

        if (stockDeducted) {
            command.setStatus(OrderSagaState.SagaStatus.INVENTORY_SUCCESS);
            command.setMessage("재고 차감 완료");
            log.info("[재고 서비스] 재고 차감 성공 - productId: {}, quantity: {}", command.getProductId(), command.getQuantity());
        } else {
            // 재고가 부족하여 실패 상태를 오케스트레이터로 반환합니다.
            command.setStatus(OrderSagaState.SagaStatus.FAILED);
            command.setMessage("재고 수량 부족 (품절)");
            log.warn("[재고 서비스] 재고 부족 실패 - productId: {}, quantity: {}", command.getProductId(), command.getQuantity());
        }

        kafkaTemplate.send("inventory-events", command.getSagaId(), command);
    }

    private synchronized boolean executeDeduct(Long productId, int quantity) {
        int currentStock = stockRepository.getOrDefault(productId, 0);
        if (currentStock < quantity) {
            return false;
        }
        stockRepository.put(productId, currentStock - quantity);
        return true;
    }

    public int getStock(Long productId) {
        return stockRepository.getOrDefault(productId, 0);
    }
}
