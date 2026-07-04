package com.stockpilot;

import com.stockpilot.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.model.Customer;
import com.stockpilot.model.Order;
import com.stockpilot.model.OrderItem;
import com.stockpilot.model.Product;
import com.stockpilot.repository.CustomerRepository;
import com.stockpilot.repository.OrderRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.service.CustomerService;
import com.stockpilot.service.OrderService;
import com.stockpilot.service.ProductService;
import com.stockpilot.service.ReportService;
import com.stockpilot.service.pricing.BulkDiscount;
import com.stockpilot.service.pricing.DiscountPolicy;
import com.stockpilot.service.pricing.NoDiscount;
import com.stockpilot.service.pricing.PercentageDiscount;
import com.stockpilot.service.pricing.PricingRule;
import com.stockpilot.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {


    private Order orderWithSubtotal(BigDecimal unitPrice, int qty) {
        Order order = new Order();
        order.setCustomerId(1L);
        order.addItem(new OrderItem(null, null, 100L, qty, unitPrice));
        return order;
    }

    @Test
    public void testNoDiscountAlwaysReturnsZero() {
        Order order = orderWithSubtotal(new BigDecimal("500.00"), 3);
        DiscountPolicy policy = new NoDiscount();

        assertEquals(0, policy.calculateDiscount(order).compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testPercentageDiscountCalculatesCorrectAmount() {
        Order order = orderWithSubtotal(new BigDecimal("100.00"), 2); // subtotal 200.00
        DiscountPolicy policy = new PercentageDiscount(new BigDecimal("10"));

        assertEquals(0, policy.calculateDiscount(order).compareTo(new BigDecimal("20.00")));
    }

    @Test
    public void testPercentageDiscountRejectsOutOfRangeValues() {
        assertThrows(InvalidInputException.class,
                () -> new PercentageDiscount(new BigDecimal("-5")));
        assertThrows(InvalidInputException.class,
                () -> new PercentageDiscount(new BigDecimal("150")));
    }

    @Test
    public void testBulkDiscountGivesNothingBelowThreshold() {
        Order order = orderWithSubtotal(new BigDecimal("50.00"), 3); // qty 3 < threshold 10
        DiscountPolicy policy = new BulkDiscount(10, new BigDecimal("15"));

        assertEquals(0, policy.calculateDiscount(order).compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testBulkDiscountAppliesOnceThresholdReached() {
        Order order = orderWithSubtotal(new BigDecimal("50.00"), 10); // subtotal 500.00, qty 10
        DiscountPolicy policy = new BulkDiscount(10, new BigDecimal("15"));

        assertEquals(0, policy.calculateDiscount(order).compareTo(new BigDecimal("75.00")));
    }

    @Test
    public void testCustomLambdaRuleWorksAsAPricingRule() {
        PricingRule flatTenOffOver100 = order ->
                order.getSubtotal().compareTo(new BigDecimal("100")) >= 0 ? new BigDecimal("10") : BigDecimal.ZERO;

        Order bigOrder = orderWithSubtotal(new BigDecimal("60.00"), 2);   // subtotal 120
        Order smallOrder = orderWithSubtotal(new BigDecimal("10.00"), 2); // subtotal 20

        assertEquals(0, flatTenOffOver100.calculateDiscount(bigOrder).compareTo(new BigDecimal("10")));
        assertEquals(0, flatTenOffOver100.calculateDiscount(smallOrder).compareTo(BigDecimal.ZERO));
    }


    private ProductService productService;
    private CustomerService customerService;
    private OrderService orderService;
    private ReportService reportService;

    @BeforeAll
    static void setUpDatabase() {
        System.setProperty("stockpilot.db.url", "jdbc:h2:mem:stockpilot_apptest;DB_CLOSE_DELAY=-1");
        DatabaseConnection.initializeDatabase();
    }

    @BeforeEach
    void setUp() {
        resetAllTables();
        ProductRepository productRepository = new ProductRepository();
        CustomerRepository customerRepository = new CustomerRepository();
        OrderRepository orderRepository = new OrderRepository();

        productService = new ProductService(productRepository);
        customerService = new CustomerService(customerRepository);
        orderService = new OrderService(orderRepository, productService, customerService);
        reportService = new ReportService(orderRepository, productRepository);
    }

    /** Wipes every table and resets auto-increment ids so each @Test starts from a clean slate. */
    private static void resetAllTables() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM order_items");
            stmt.execute("DELETE FROM orders");
            stmt.execute("DELETE FROM products");
            stmt.execute("DELETE FROM customers");
            stmt.execute("ALTER TABLE order_items ALTER COLUMN id RESTART WITH 1");
            stmt.execute("ALTER TABLE orders ALTER COLUMN id RESTART WITH 1");
            stmt.execute("ALTER TABLE products ALTER COLUMN id RESTART WITH 1");
            stmt.execute("ALTER TABLE customers ALTER COLUMN id RESTART WITH 1");
        } catch (SQLException e) {
            throw new RuntimeException("Could not reset test database", e);
        }
    }

    @Test
    public void testPlaceOrderDecrementsStockAndAppliesDiscount() {
        Customer customer = customerService.createCustomer("Alice", "alice@example.com", "0912345678");
        Product product = productService.createProduct("ABC-1234", "Widget", "Tools", new BigDecimal("100.00"), 10);

        Map<String, Integer> cart = new LinkedHashMap<>();
        cart.put(product.getSku(), 2);

        Order order = orderService.placeOrder(customer.getId(), cart, new PercentageDiscount(new BigDecimal("10")));

        assertEquals(0, order.getSubtotal().compareTo(new BigDecimal("200.00")));
        assertEquals(0, order.getDiscountAmount().compareTo(new BigDecimal("20.00")));
        assertEquals(0, order.getTotalAmount().compareTo(new BigDecimal("180.00")));

        Product reloaded = productService.findById(product.getId());
        assertEquals(8, reloaded.getStockQuantity());
    }

    @Test
    public void testPlaceOrderThrowsWhenStockIsInsufficient() {
        Customer customer = customerService.createCustomer("Bob", "bob@example.com", "0912345679");
        Product product = productService.createProduct("XYZ-9999", "Gadget", "Tools", new BigDecimal("50.00"), 2);

        Map<String, Integer> cart = new LinkedHashMap<>();
        cart.put(product.getSku(), 5); // only 2 in stock

        assertThrows(InsufficientStockException.class, () -> orderService.placeOrder(customer.getId(), cart));

        Product reloaded = productService.findById(product.getId());
        assertEquals(2, reloaded.getStockQuantity());
        assertTrue(orderService.listAll().isEmpty());
    }

    @Test
    public void testPlaceOrderIsAtomicAcrossMultipleItems() {
        Customer customer = customerService.createCustomer("Carol", "carol@example.com", "0912345680");
        Product plentiful = productService.createProduct("AAA-1111", "Plenty", "Tools", new BigDecimal("10.00"), 100);
        Product scarce = productService.createProduct("BBB-2222", "Scarce", "Tools", new BigDecimal("20.00"), 1);

        Map<String, Integer> cart = new LinkedHashMap<>();
        cart.put(plentiful.getSku(), 5);
        cart.put(scarce.getSku(), 10); // fails: only 1 in stock

        assertThrows(InsufficientStockException.class, () -> orderService.placeOrder(customer.getId(), cart));

        // A forced mid-order failure must leave everything as it was (F3 Excellent tier).
        assertEquals(100, productService.findById(plentiful.getId()).getStockQuantity());
        assertEquals(1, productService.findById(scarce.getId()).getStockQuantity());
        assertTrue(orderService.listAll().isEmpty());
    }

    @Test
    public void testTopSellingProductsOrdersByQuantityDescending() {
        Customer customer = customerService.createCustomer("Dan", "dan@example.com", "0900000001");
        Product popular = productService.createProduct("POP-0001", "Popular", "Cat A", new BigDecimal("10.00"), 100);
        Product niche = productService.createProduct("NCH-0002", "Niche", "Cat A", new BigDecimal("10.00"), 100);

        orderService.placeOrder(customer.getId(), Map.of(popular.getSku(), 8));
        orderService.placeOrder(customer.getId(), Map.of(niche.getSku(), 2));

        List<Map.Entry<Product, Long>> top = reportService.topSellingProducts(1);

        assertEquals(1, top.size());
        assertEquals(popular.getSku(), top.get(0).getKey().getSku());
        assertEquals(8L, top.get(0).getValue());
    }

    @Test
    public void testLowStockProductsFiltersBelowThreshold() {
        productService.createProduct("LOW-0001", "Almost gone", "Cat A", new BigDecimal("5.00"), 2);
        productService.createProduct("OKY-0002", "Well stocked", "Cat A", new BigDecimal("5.00"), 50);

        List<Product> lowStock = reportService.lowStockProducts(10);

        assertEquals(1, lowStock.size());
        assertEquals("LOW-0001", lowStock.get(0).getSku());
    }

    @Test
    public void testRevenueByCategoryGroupsCorrectly() {
        Customer customer = customerService.createCustomer("Eve", "eve@example.com", "0900000002");
        Product a = productService.createProduct("CTA-0001", "Item A", "Category A", new BigDecimal("10.00"), 100);
        Product b = productService.createProduct("CTB-0002", "Item B", "Category B", new BigDecimal("20.00"), 100);

        orderService.placeOrder(customer.getId(), Map.of(a.getSku(), 3)); // 30.00 in Category A
        orderService.placeOrder(customer.getId(), Map.of(b.getSku(), 2)); // 40.00 in Category B

        Map<String, BigDecimal> byCategory = reportService.revenueByCategory();

        assertEquals(0, byCategory.get("Category A").compareTo(new BigDecimal("30.00")));
        assertEquals(0, byCategory.get("Category B").compareTo(new BigDecimal("40.00")));
    }
}
