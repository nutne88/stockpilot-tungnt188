package vn.edu.fpt.service;

import vn.edu.fpt.exception.InvalidInputException;
import vn.edu.fpt.model.Order;
import vn.edu.fpt.model.OrderItem;
import vn.edu.fpt.model.Product;
import vn.edu.fpt.repository.OrderRepository;
import vn.edu.fpt.service.pricing.NoDiscount;
import vn.edu.fpt.service.pricing.PricingRule;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
        if (discountRule == null) {
            discountRule = new NoDiscount();
        }

        customerService.findById(customerId);

        Order order = new Order();
        order.setCustomerId(customerId);

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String sku = entry.getKey();
            int qty = entry.getValue();
            if (qty <= 0) {
                throw new InvalidInputException("Quantity for " + sku + " must be positive.");
            }

            Product product = productService.findBySku(sku);
            productService.ensureStockAvailable(product, qty);

            order.addItem(new OrderItem(null, null, product.getId(), qty, product.getPrice()));
        }

        BigDecimal discount = discountRule.calculateDiscount(order);
        order.applyDiscount(discount);

        orderRepository.save(order);
        return order;
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