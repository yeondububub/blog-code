package com.example.orderservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "order.discount.rate=12.5"
})
class OrderPropertiesTest {

    @Autowired
    private OrderProperties orderProperties;

    @Test
    @DisplayName("환경 설정 프로퍼티가 정상적으로 주입된다")
    void verifyDiscountRateInjection() {
        assertThat(orderProperties.getDiscountRate()).isEqualTo(12.5);
    }
}
