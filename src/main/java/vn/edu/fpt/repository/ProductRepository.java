package vn.edu.fpt.repository;

import vn.edu.fpt.exception.DataAccessException;
import vn.edu.fpt.exception.ProductNotFoundException;
import vn.edu.fpt.model.Product;
import vn.edu.fpt.util.DatabaseConnection;

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
}