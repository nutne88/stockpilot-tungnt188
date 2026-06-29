package vn.edu.fpt;

import vn.edu.fpt.model.Product;
import vn.edu.fpt.model.Customer;
import vn.edu.fpt.repository.ProductRepository;
import vn.edu.fpt.repository.CustomerRepository;
import vn.edu.fpt.util.DatabaseConnection;

import java.math.BigDecimal;

public class App {
    public static void main(String[] args) {
        System.out.println("=== INITIALIZING STOCKPILOT SYSTEM ===");

        DatabaseConnection.initializeDatabase();

        ProductRepository productRepo = new ProductRepository();
        CustomerRepository customerRepo = new CustomerRepository();

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

        System.out.println("\n=== DAY 2 COMPLETED SUCCESSFULLY ===");
    }
}