package vn.edu.fpt.repository;

import vn.edu.fpt.util.DatabaseConnection;
import vn.edu.fpt.exception.DataAccessException;

import java.sql.*;

public class ReportRepository {

    public void printRevenueReport() {
        String sql = "SELECT COUNT(id) AS total_orders, SUM(total_amount) AS total_revenue FROM orders";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n========================================");
            System.out.println("        REVENUE & SALES REPORT          ");
            System.out.println("========================================");
            if (rs.next()) {
                int totalOrders = rs.getInt("total_orders");
                double totalRevenue = rs.getDouble("total_revenue");

                System.out.printf("Total Orders Placed : %d\n", totalOrders);
                System.out.printf("Total Revenue Earned: $%.2f\n", totalRevenue);
            }
            System.out.println("========================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error generating revenue report", e);
        }
    }

    public void printTopSellingProducts() {
        String sql = """
            SELECT p.sku, p.name, SUM(oi.quantity) AS total_sold 
            FROM order_items oi
            INNER JOIN products p ON oi.product_id = p.id
            GROUP BY p.id, p.sku, p.name
            ORDER BY total_sold DESC
            LIMIT 5
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n==================================================");
            System.out.println("         TOP 5 BEST SELLING PRODUCTS              ");
            System.out.println("==================================================");
            System.out.printf("%-12s | %-22s | %-10s\n", "SKU", "Product Name", "Qty Sold");
            System.out.println("--------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-12s | %-22s | %-10d\n",
                        rs.getString("sku"),
                        rs.getString("name"),
                        rs.getInt("total_sold"));
            }
            System.out.println("==================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error generating top selling report", e);
        }
    }

    public void printLowStockAlert() {
        String sql = "SELECT sku, name, stock_quantity FROM products WHERE stock_quantity < 10";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n===============================================");
            System.out.println("           LOW STOCK ALERT (UNDER 10)          ");
            System.out.println("===============================================");
            System.out.printf("%-12s | %-22s | %-14s\n", "SKU", "Product Name", "Current Stock");
            System.out.println("--------------------------------------------------");

            boolean hasLowStock = false;
            while (rs.next()) {
                hasLowStock = true;
                System.out.printf("%-12s | %-22s | %-14d\n",
                        rs.getString("sku"),
                        rs.getString("name"),
                        rs.getInt("stock_quantity"));
            }

            if (!hasLowStock) {
                System.out.println("All products are well-stocked! No alerts.");
            }
            System.out.println("==================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error generating low stock alert", e);
        }
    }
}