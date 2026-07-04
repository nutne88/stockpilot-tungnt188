package com.stockpilot.service.pricing;

import com.stockpilot.exception.InvalidInputException;
import com.stockpilot.model.Order;
import com.stockpilot.model.OrderItem;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BulkDiscount extends DiscountPolicy {

    private final int minQuantity;
    private final BigDecimal percentage;

    public BulkDiscount(int minQuantity, BigDecimal percentage) {
        if (minQuantity <= 0) {
            throw new InvalidInputException("Bulk discount threshold quantity must be positive.");
        }
        if (percentage == null
                || percentage.compareTo(BigDecimal.ZERO) < 0
                || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new InvalidInputException("Discount percentage must be between 0 and 100.");
        }
        this.minQuantity = minQuantity;
        this.percentage = percentage;
    }

    @Override
    public BigDecimal calculateDiscount(Order order) {
        int totalQuantity = order.getItems().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        if (totalQuantity < minQuantity) {
            return BigDecimal.ZERO;
        }
        return order.getSubtotal()
                .multiply(percentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    @Override
    public String describe() {
        return "Bulk discount: " + percentage.stripTrailingZeros().toPlainString()
                + "% off orders of " + minQuantity + "+ items";
    }
}