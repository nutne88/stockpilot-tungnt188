package com.stockpilot.service.pricing;

import com.stockpilot.exception.InvalidInputException;
import com.stockpilot.model.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PercentageDiscount extends DiscountPolicy {

    private final BigDecimal percentage;

    public PercentageDiscount(BigDecimal percentage) {
        if (percentage == null
                || percentage.compareTo(BigDecimal.ZERO) < 0
                || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new InvalidInputException("Discount percentage must be between 0 and 100.");
        }
        this.percentage = percentage;
    }

    @Override
    public BigDecimal calculateDiscount(Order order) {
        return order.getSubtotal()
                .multiply(percentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    @Override
    public String describe() {
        return percentage.stripTrailingZeros().toPlainString() + "% discount";
    }
}