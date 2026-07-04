package com.stockpilot.service.pricing;

import com.stockpilot.model.Order;

import java.math.BigDecimal;

public class NoDiscount extends DiscountPolicy {

    @Override
    public BigDecimal calculateDiscount(Order order) {
        return BigDecimal.ZERO;
    }

    @Override
    public String describe() {
        return "No discount";
    }
}