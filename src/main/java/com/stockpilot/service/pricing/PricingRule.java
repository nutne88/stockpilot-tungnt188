package com.stockpilot.service.pricing;

import com.stockpilot.model.Order;

import java.math.BigDecimal;

@FunctionalInterface
public interface PricingRule {
    BigDecimal calculateDiscount(Order order);
}