package com.stockpilot.service;

import com.stockpilot.model.Order;
import com.stockpilot.model.OrderItem;
import com.stockpilot.model.Product;
import com.stockpilot.repository.OrderRepository;
import com.stockpilot.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public ReportService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public BigDecimal totalRevenue() {
        return orderRepository.findAll().stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long totalOrderCount() {
        return orderRepository.findAll().size();
    }

    public List<Map.Entry<Product, Long>> topSellingProducts(int topN) {
        Map<Long, Product> productsById = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, Long> quantityByProductId = orderRepository.findAll().stream()
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.groupingBy(
                        OrderItem::getProductId,
                        Collectors.summingLong(OrderItem::getQuantity)));

        return quantityByProductId.entrySet().stream()
                .filter(e -> productsById.containsKey(e.getKey()))
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(topN)
                .map(e -> Map.entry(productsById.get(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

    public Map<String, BigDecimal> revenueByCategory() {
        Map<Long, Product> productsById = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        return orderRepository.findAll().stream()
                .flatMap(order -> order.getItems().stream())
                .filter(item -> productsById.containsKey(item.getProductId()))
                .collect(Collectors.groupingBy(
                        item -> productsById.get(item.getProductId()).getCategory(),
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                                BigDecimal::add)));
    }

    public List<Product> lowStockProducts(int threshold) {
        return productRepository.findAll().stream()
                .filter(p -> p.getStockQuantity() < threshold)
                .sorted(Comparator.comparingInt(Product::getStockQuantity))
                .collect(Collectors.toList());
    }
}