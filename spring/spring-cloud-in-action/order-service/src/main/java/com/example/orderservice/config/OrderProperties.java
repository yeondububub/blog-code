package com.example.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 동적 설정 프로퍼티 관리 컴포넌트
 */
@Component
@RefreshScope // Actuator /refresh 요청 시 빈을 재생성하여 최신 설정값을 바인딩합니다.
public class OrderProperties {

    // Config Server에서 관리 중인 프로퍼티 값을 주입받습니다.
    @Value("${order.discount.rate:0.0}")
    private double discountRate;

    public double getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(double discountRate) {
        this.discountRate = discountRate;
    }
}
