package vn.edu.fpt;

import vn.edu.fpt.model.Product;
import vn.edu.fpt.repository.ProductRepository;
import vn.edu.fpt.util.DatabaseConnection;

import java.math.BigDecimal;
import java.util.List;

public class App {
    public static void main(String[] args) {
        System.out.println("=== INITIALIZING STOCKPILOT SYSTEM ===");

        DatabaseConnection.initializeDatabase();

        ProductRepository productRepo = new ProductRepository();

        System.out.println("\n--- Inserting Test Product ---");
        try {
            String randomSku = "IPH-" + (int) (Math.random() * 9000 + 1000);
            Product newProduct = new Product(
                    null,
                    randomSku,
                    "iPhone 15 Pro Max",
                    "Electronics",
                    new BigDecimal("30000000.00"),
                    50
            );
            productRepo.save(newProduct);
        } catch (Exception e) {
            System.err.println("Failed to insert product: " + e.getMessage());
        }

        System.out.println("\n--- Current Product Catalog in DB ---");
        List<Product> allProducts = productRepo.findAll();
        for (Product p : allProducts) {
            System.out.println(p);
        }
    }
}