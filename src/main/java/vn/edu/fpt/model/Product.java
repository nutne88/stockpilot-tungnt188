package vn.edu.fpt.model;

import java.math.BigDecimal;
import vn.edu.fpt.exception.InvalidInputException;

public class Product {
    private Long id;
    private String sku;
    private String name;
    private String category;
    private BigDecimal price;
    private int stockQuantity;

    public Product() {
    }

    public Product(Long id, String sku, String name, String category, BigDecimal price, int stockQuantity) {
        if (sku == null || !sku.matches("^[A-Z]{3}-\\d{4}$")) {
            throw new InvalidInputException(
                    "Invalid SKU format! Expected format: AAA-1234 (e.g., IPH-1500). Got: " + sku);
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidInputException("Price cannot be negative!");
        }
        if (stockQuantity < 0) {
            throw new InvalidInputException("Stock quantity cannot be negative!");
        }

        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    @Override
    public String toString() {
        return String.format("ID: %d | SKU: %s | Name: %s | Category: %s | Price: %s VNĐ | Stock: %d",
                id, sku, name, category, price.toString(), stockQuantity);
    }
}