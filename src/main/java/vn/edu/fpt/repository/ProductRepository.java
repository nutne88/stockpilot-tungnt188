package vn.edu.fpt.repository;

import vn.edu.fpt.exception.DataAccessException;
import vn.edu.fpt.model.Product;
import vn.edu.fpt.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductRepository implements Repository<Product, Long> {

    @Override
    public void save(Product product) {
        String sql = "INSERT INTO products (sku, name, category, price, stock_quantity) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, product.getSku());
            pstmt.setString(2, product.getName());
            pstmt.setString(3, product.getCategory());
            pstmt.setBigDecimal(4, product.getPrice());
            pstmt.setInt(5, product.getStockQuantity());

            pstmt.executeUpdate();
            System.out.println("Save product successfully.");
        } catch (SQLException e) {
            throw new DataAccessException("Error: ", e);
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
                Product product = new Product(
                        rs.getLong("id"),
                        rs.getString("sku"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getBigDecimal("price"),
                        rs.getInt("stock_quantity")
                );
                products.add(product);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error : ", e);
        }
        return products;
    }
}