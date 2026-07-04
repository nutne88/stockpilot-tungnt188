package com.stockpilot.repository;

import com.stockpilot.exception.DataAccessException;
import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.exception.ProductNotFoundException;
import com.stockpilot.model.Product;
import com.stockpilot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductRepository implements Repository<Product, Long> {

    @Override
    public void save(Product product) {
        String sql = "INSERT INTO products (sku, name, category, price, stock_quantity) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, product.getSku());
            pstmt.setString(2, product.getName());
            pstmt.setString(3, product.getCategory());
            pstmt.setBigDecimal(4, product.getPrice());
            pstmt.setInt(5, product.getStockQuantity());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setId(generatedKeys.getLong(1));
                }
            }
            System.out.println("Save product successfully.");
        } catch (SQLException e) {
            throw new DataAccessException("Error saving product to database", e);
        }
    }

    @Override
    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error fetching product list", e);
        }
        return products;
    }

    @Override
    public Optional<Product> findById(Long id) {
        String sql = "SELECT * FROM products WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error fetching product with id " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public void update(Product product) {
        if (product.getId() == null) {
            throw new ProductNotFoundException("Cannot update a product with no id.");
        }
        // Confirm the row exists first so we can report a clear, specific error
        // instead of a silent no-op UPDATE.
        findById(product.getId())
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product with id " + product.getId() + " does not exist."));

        String sql = "UPDATE products SET sku = ?, name = ?, category = ?, price = ?, stock_quantity = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, product.getSku());
            pstmt.setString(2, product.getName());
            pstmt.setString(3, product.getCategory());
            pstmt.setBigDecimal(4, product.getPrice());
            pstmt.setInt(5, product.getStockQuantity());
            pstmt.setLong(6, product.getId());

            pstmt.executeUpdate();
            System.out.println("Product updated successfully.");
        } catch (SQLException e) {
            throw new DataAccessException("Error updating product with id " + product.getId(), e);
        }
    }

    @Override
    public void deleteById(Long id) {
        findById(id).orElseThrow(() ->
                new ProductNotFoundException("Product with id " + id + " does not exist."));

        String sql = "DELETE FROM products WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            System.out.println("Product deleted successfully.");
        } catch (SQLException e) {
            throw new DataAccessException("Error deleting product with id " + id, e);
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getLong("id"),
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getBigDecimal("price"),
                rs.getInt("stock_quantity")
        );
    }

    public void decrementStockCheckThenAct(Long productId, int quantity, long simulatedDelayMillis) {
        String selectSql = "SELECT stock_quantity FROM products WHERE id = ?";
        String updateSql = "UPDATE products SET stock_quantity = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            int currentStock;
            try (PreparedStatement select = conn.prepareStatement(selectSql)) {
                select.setLong(1, productId);
                try (ResultSet rs = select.executeQuery()) {
                    if (!rs.next()) {
                        throw new ProductNotFoundException("Product with id " + productId + " does not exist.");
                    }
                    currentStock = rs.getInt("stock_quantity");
                }
            }

            if (simulatedDelayMillis > 0) {
                try {
                    Thread.sleep(simulatedDelayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            int newStock = currentStock - quantity;
            if (newStock < 0) {
                throw new InsufficientStockException(
                        "Not enough stock for product " + productId + " (available " + currentStock + ", requested " + quantity + ")");
            }

            try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                update.setInt(1, newStock);
                update.setLong(2, productId);
                update.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error decrementing stock for product " + productId, e);
        }
    }
}