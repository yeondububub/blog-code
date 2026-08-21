package com.example.springsagaorchestrator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {
        "payment-commands",
        "payment-events",
        "inventory-commands",
        "inventory-events",
        "payment-compensate-commands",
        "order-cancel-commands",
        "order-complete-commands"
})
class SpringSagaOrchestratorApplicationTests {

    @Test
    void contextLoads() {
    }
}
