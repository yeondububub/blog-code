package com.example.springsagaorchestrator.config;

import com.example.springsagaorchestrator.account.domain.UserAccount;
import com.example.springsagaorchestrator.account.repository.UserAccountRepository;
import com.example.springsagaorchestrator.inventory.domain.Inventory;
import com.example.springsagaorchestrator.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 애플리케이션 기동 시 테스트용 사용자 계좌 및 상품 재고 초기 데이터 적재
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserAccountRepository userAccountRepository;
    private final InventoryRepository inventoryRepository;

    public DataInitializer(UserAccountRepository userAccountRepository, InventoryRepository inventoryRepository) {
        this.userAccountRepository = userAccountRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void run(String... args) {
        // 1. 사용자 계좌 시드 데이터
        if (userAccountRepository.count() == 0) {
            // 사용자 1번: 넉넉한 잔액 (100,000원) - 정상 주문 및 보상 트랜잭션 환불 테스트용
            userAccountRepository.save(new UserAccount(1L, "110-123-456789", BigDecimal.valueOf(150_000)));

            // 사용자 2번: 부족한 잔액 (5,000원) - 결제 잔액 부족 실패 테스트용
            userAccountRepository.save(new UserAccount(2L, "110-999-888888", BigDecimal.valueOf(5_000)));

            log.info("[초기 데이터] 사용자 계좌 초기화 완료 (User 1: 100,000원, User 2: 5,000원)");
        }

        // 2. 상품 재고 시드 데이터
        if (inventoryRepository.count() == 0) {
            // 상품 1번: 넉넉한 재고 (100개)
            inventoryRepository.save(new Inventory(1L, "RTX 5060", BigDecimal.valueOf(50_000), 100));

            // 상품 2번: 소량 재고 (1개)
            inventoryRepository.save(new Inventory(2L, "RTX 5070", BigDecimal.valueOf(30_000), 1));

            // 상품 999번: 품절 상품 (0개) - 재고 부족으로 인한 결제 환불 보상 트랜잭션 테스트용
            inventoryRepository.save(new Inventory(999L, "RTX 5090", BigDecimal.valueOf(70_000), 0));

            log.info("[초기 데이터] 상품 재고 초기화 완료 (Product 1: 100개, Product 2: 1개, Product 999: 0개 품절)");
        }
    }
}
