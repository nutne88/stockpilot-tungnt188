package vn.edu.fpt;

import vn.edu.fpt.model.Product;
import vn.edu.fpt.model.Customer;
import vn.edu.fpt.model.Order;
import vn.edu.fpt.model.OrderItem;
import vn.edu.fpt.repository.ProductRepository;
import vn.edu.fpt.repository.CustomerRepository;
import vn.edu.fpt.repository.OrderRepository;
import vn.edu.fpt.repository.ReportRepository;
import vn.edu.fpt.util.DatabaseConnection;

import java.math.BigDecimal;

public class App {
    public static void main(String[] args) {
        System.out.println("=== INITIALIZING STOCKPILOT SYSTEM ===");

        DatabaseConnection.initializeDatabase();

        ProductRepository productRepo = new ProductRepository();
        CustomerRepository customerRepo = new CustomerRepository();
        OrderRepository orderRepo = new OrderRepository();
        ReportRepository reportRepo = new ReportRepository(); // Khởi tạo Report Repo

        System.out.println("\n--- Testing Product SKU Validation ---");
        try {
            Product invalidProduct = new Product(null, "iph-123", "Wrong Product", "Test", new BigDecimal("100"), 10);
            productRepo.save(invalidProduct);
        } catch (Exception e) {
            System.out.println("Success! System blocked invalid product: " + e.getMessage());
        }

        System.out.println("\n--- Inserting Test Customer ---");
        try {
            String randomEmail = "tungnt" + (int) (Math.random() * 10000) + "@fpt.edu.vn";
            Customer newCustomer = new Customer(null, "Tung Nguyen", randomEmail, "0912345678");
            customerRepo.save(newCustomer);
        } catch (Exception e) {
            System.err.println("Failed to insert customer: " + e.getMessage());
        }

        System.out.println("\n--- Current Customer List in DB ---");
        customerRepo.findAll().forEach(System.out::println);

        System.out.println("\n--- Preparing Mock Data for Order ---");
        String uniqueSku = "SAM-" + (int)(Math.random() * 8999 + 1000);
        Product phone = new Product(null, uniqueSku, "Samsung S26 Ultra", "Tech", new BigDecimal("1200"), 50);
        productRepo.save(phone);

        Product savedPhone = productRepo.findAll().stream()
                .filter(p -> p.getSku().equals(uniqueSku)).findFirst().orElse(null);
        Customer currentCustomer = customerRepo.findAll().stream().findFirst().orElse(null);

        if (savedPhone != null && currentCustomer != null) {
            System.out.println("\n--- Test Case 1: Processing Valid Order (Buy 5) ---");
            try {
                Order order1 = new Order();
                order1.setCustomerId(currentCustomer.getId());
                order1.addItem(new OrderItem(null, null, savedPhone.getId(), 5, savedPhone.getPrice()));

                orderRepo.executeOrderTransaction(order1);
            } catch (Exception e) {
                System.err.println("Order 1 Failed: " + e.getMessage());
            }

            System.out.println("\n--- Test Case 2: Processing Invalid Order (Buy 100 - Overstock) ---");
            try {
                Order order2 = new Order();
                order2.setCustomerId(currentCustomer.getId());
                order2.addItem(new OrderItem(null, null, savedPhone.getId(), 100, savedPhone.getPrice()));

                orderRepo.executeOrderTransaction(order2);
            } catch (Exception e) {
                System.out.println("Success! System blocked overstock order: " + e.getMessage());
            }
        }

        System.out.println("\n--- RUNNING SYSTEM ANALYTICS & REPORTS ---");
        reportRepo.printRevenueReport();
        reportRepo.printTopSellingProducts();

        Product lowStockProduct = new Product(null, "LOW-9999", "Out of Stock Item", "Test", new BigDecimal("10"), 3);
        productRepo.save(lowStockProduct);
        reportRepo.printLowStockAlert();

        System.out.println("\n=== DAY 4 COMPLETED SUCCESSFULLY ===");
    }
}