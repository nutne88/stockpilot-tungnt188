package com.stockpilot.repository;

import com.stockpilot.exception.DataAccessException;
import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.model.Order;
import com.stockpilot.model.OrderItem;
import com.stockpilot.util.DatabaseConnection;

import java.sql.*;
import java.util.*;

public class OrderRepository implements Repository<Order, Long> {

    @Override
    public void save(Order order) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String insertOrderSql = "INSERT INTO orders (customer_id, order_date, subtotal, discount_amount, total_amount) VALUES (?, ?, ?, ?, ?)";
            Long orderId = null;
            try (PreparedStatement pstmt = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setLong(1, order.getCustomerId());
                pstmt.setTimestamp(2, Timestamp.valueOf(order.getOrderDate()));
                pstmt.setBigDecimal(3, order.getSubtotal());
                pstmt.setBigDecimal(4, order.getDiscountAmount());
                pstmt.setBigDecimal(5, order.getTotalAmount());
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
                                throw new InsufficientStockException(
                                        "Not enough stock for product: " + prodName + " (available: " + currentStock + ")");
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

            order.setId(orderId);
            conn.commit();

        } catch (InsufficientStockException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(conn);
            throw new DataAccessException("Order processing failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public List<Order> findAll() {
        Map<Long, Order> ordersById = new LinkedHashMap<>();

        String orderSql = "SELECT * FROM orders ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(orderSql)) {

            while (rs.next()) {
                Order order = new Order(
                        rs.getLong("id"),
                        rs.getLong("customer_id"),
                        rs.getTimestamp("order_date").toLocalDateTime(),
                        rs.getBigDecimal("subtotal"),
                        rs.getBigDecimal("discount_amount"),
                        rs.getBigDecimal("total_amount"));
                ordersById.put(order.getId(), order);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error fetching orders", e);
        }

        String itemSql = "SELECT * FROM order_items ORDER BY order_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(itemSql)) {

            while (rs.next()) {
                Order order = ordersById.get(rs.getLong("order_id"));
                if (order != null) {
                    order.getItems().add(new OrderItem(
                            rs.getLong("id"),
                            rs.getLong("order_id"),
                            rs.getLong("product_id"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("unit_price")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error fetching order items", e);
        }

        return new ArrayList<>(ordersById.values());
    }

    @Override
    public Optional<Order> findById(Long id) {
        return findAll().stream()
                .filter(o -> o.getId().equals(id))
                .findFirst();
    }

    @Override
    public void update(Order order) {
        throw new UnsupportedOperationException("Orders are immutable once placed; there is no update flow.");
    }

    @Override
    public void deleteById(Long id) {
        throw new UnsupportedOperationException("Orders cannot be deleted; they are the audit trail.");
    }

    private void rollbackQuietly(Connection conn) {
        if (conn == null) return;
        try {
            conn.rollback();
            System.err.println("[Transaction] Rolled back — stock and order data unchanged.");
        } catch (SQLException ex) {
            System.err.println("[Transaction] Rollback failed: " + ex.getMessage());
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn == null) return;
        try {
            conn.setAutoCommit(true);
            conn.close();
        } catch (SQLException e) {
            System.err.println("[Transaction] Error closing connection: " + e.getMessage());
        }
    }
}