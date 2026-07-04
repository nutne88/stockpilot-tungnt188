package vn.edu.fpt.service.pricing;

import vn.edu.fpt.model.Order;

import java.math.BigDecimal;

@FunctionalInterface
public interface PricingRule {
    BigDecimal calculateDiscount(Order order);
}