package com.stockpilot.io;

import com.stockpilot.model.Order;
import com.stockpilot.model.OrderItem;
import com.stockpilot.model.Product;
import com.stockpilot.service.ProductService;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

public class InvoiceExporter {

    private static final Path OUTPUT_DIR = Path.of("output");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ProductService productService;

    public InvoiceExporter(ProductService productService) {
        this.productService = productService;
    }

    public Path export(Order order) throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        Path file = OUTPUT_DIR.resolve("invoice-" + order.getId() + "-" + order.getOrderDate().format(DATE_FORMAT) + ".txt");

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
            writer.println("========================================");
            writer.println("              STOCKPILOT INVOICE          ");
            writer.println("========================================");
            writer.printf("Order #      : %d%n", order.getId());
            writer.printf("Customer id  : %d%n", order.getCustomerId());
            writer.printf("Date         : %s%n", order.getOrderDate());
            writer.println("----------------------------------------");
            writer.printf("%-10s %-20s %6s %10s %10s%n", "SKU", "Name", "Qty", "Unit", "Line total");
            for (OrderItem item : order.getItems()) {
                String sku = "#" + item.getProductId();
                String name = "";
                try {
                    Product product = productService.findById(item.getProductId());
                    sku = product.getSku();
                    name = product.getName();
                } catch (Exception ignored) {
                }
                writer.printf("%-10s %-20s %6d %10s %10s%n",
                        sku, name, item.getQuantity(), item.getUnitPrice(),
                        item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
            }
            writer.println("----------------------------------------");
            writer.printf("Subtotal     : %s%n", order.getSubtotal());
            writer.printf("Discount     : -%s%n", order.getDiscountAmount());
            writer.printf("TOTAL        : %s%n", order.getTotalAmount());
            writer.println("========================================");
        }

        return file;
    }
}