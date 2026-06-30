package vn.edu.fpt.repository;

import vn.edu.fpt.model.Order;
import vn.edu.fpt.model.OrderItem;
import vn.edu.fpt.util.DatabaseConnection;
import vn.edu.fpt.exception.DataAccessException;
import vn.edu.fpt.exception.InvalidInputException;

import java.sql.*;

public class OrderRepository {

    public void executeOrderTransaction(Order order) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String insertOrderSql = "INSERT INTO orders (customer_id, order_date, total_amount) VALUES (?, ?, ?)";
            Long orderId = null;
            try (PreparedStatement pstmt = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setLong(1, order.getCustomerId());
                pstmt.setTimestamp(2, Timestamp.valueOf(order.getOrderDate()));
                pstmt.setBigDecimal(3, order.getTotalAmount());
                pstmt.executeUpdate();

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        orderId = generatedKeys.getLong(1);
                    }
                }
            }

            String checkStockSql = "SELECT stock_quantity, name FROM products WHERE id = ?";
            String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ?";
            String insertItemSql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";

            for (OrderItem item : order.getItems()) {
                try (PreparedStatement checkStmt = conn.prepareStatement(checkStockSql)) {
                    checkStmt.setLong(1, item.getProductId());
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            int currentStock = rs.getInt("stock_quantity");
                            String prodName = rs.getString("name");
                            if (currentStock < item.getQuantity()) {
                                throw new InvalidInputException("Stocks not enough for product: " + prodName + " (Stocks: " + currentStock + ")");
                            }
                        }
                    }
                }

                try (PreparedStatement updateStmt = conn.prepareStatement(updateStockSql)) {
                    updateStmt.setInt(1, item.getQuantity());
                    updateStmt.setLong(2, item.getProductId());
                    updateStmt.executeUpdate();
                }

                try (PreparedStatement itemStmt = conn.prepareStatement(insertItemSql)) {
                    itemStmt.setLong(1, orderId);
                    itemStmt.setLong(2, item.getProductId());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setBigDecimal(4, item.getUnitPrice());
                    itemStmt.executeUpdate();
                }
            }

            conn.commit();
            System.out.println("[Transaction] Order processed and stock deducted successfully.");

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("[Transaction] Transaction rolled back due to error!");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new DataAccessException("Order processing failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}