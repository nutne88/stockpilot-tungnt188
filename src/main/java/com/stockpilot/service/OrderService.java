package com.stockpilot.service;

import com.stockpilot.concurrent.StockLockManager;
import com.stockpilot.exception.InvalidInputException;
import com.stockpilot.model.Order;
import com.stockpilot.model.OrderItem;
import com.stockpilot.model.Product;
import com.stockpilot.repository.OrderRepository;
import com.stockpilot.service.pricing.NoDiscount;
import com.stockpilot.service.pricing.PricingRule;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final CustomerService customerService;

    public OrderService(OrderRepository orderRepository, ProductService productService, CustomerService customerService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.customerService = customerService;
    }

    public Order placeOrder(Long customerId, Map<String, Integer> cart) {
        return placeOrder(customerId, cart, new NoDiscount());
    }

    public Order placeOrder(Long customerId, Map<String, Integer> cart, PricingRule discountRule) {
        if (cart == null || cart.isEmpty()) {
            throw new InvalidInputException("Cart is empty.");
        }
        PricingRule rule = discountRule == null ? new NoDiscount() : discountRule;

        customerService.findById(customerId);

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            if (entry.getValue() <= 0) {
                throw new InvalidInputException("Quantity for " + entry.getKey() + " must be positive.");
            }
        }

        List<Product> products = cart.keySet().stream()
                .map(productService::findBySku)
                .sorted(Comparator.comparing(Product::getId))
                .collect(Collectors.toList());

        List<ReentrantLock> locks = products.stream()
                .map(p -> StockLockManager.lockFor(p.getId()))
                .collect(Collectors.toList());

        locks.forEach(ReentrantLock::lock);
        try {
            Order order = new Order();
            order.setCustomerId(customerId);

            for (Product product : products) {
                int qty = cart.get(product.getSku());
                Product fresh = productService.findById(product.getId());
                productService.ensureStockAvailable(fresh, qty);
                order.addItem(new OrderItem(null, null, fresh.getId(), qty, fresh.getPrice()));
            }

            BigDecimal discount = rule.calculateDiscount(order);
            order.applyDiscount(discount);

            orderRepository.save(order);
            return order;
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--) {
                locks.get(i).unlock();
            }
        }
    }

    public List<Order> listAll() {
        return orderRepository.findAll();
    }

    public List<Order> listAllSortedByDateDesc() {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(Order::getOrderDate).reversed())
                .collect(Collectors.toList());
    }
}