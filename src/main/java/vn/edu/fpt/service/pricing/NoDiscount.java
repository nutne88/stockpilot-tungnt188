package vn.edu.fpt.service.pricing;

import vn.edu.fpt.model.Order;

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