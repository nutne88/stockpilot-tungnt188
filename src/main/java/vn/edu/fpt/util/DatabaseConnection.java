package vn.edu.fpt.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String URL = "jdbc:h2:./data/stockpilot_db;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initializeDatabase() {
        String createProductTable = """
                    CREATE TABLE IF NOT EXISTS products (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        sku VARCHAR(20) NOT NULL UNIQUE,
                        name VARCHAR(100) NOT NULL,
                        category VARCHAR(50),
                        price DECIMAL(15, 2) NOT NULL,
                        stock_quantity INT NOT NULL
                    );
                """;

        String createCustomerTable = """
                    CREATE TABLE IF NOT EXISTS customers (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(100) NOT NULL UNIQUE,
                        phone VARCHAR(20) NOT NULL
                    );
                """;
        String createOrderTable = """
                    CREATE TABLE IF NOT EXISTS orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        customer_id BIGINT NOT NULL,
                        order_date TIMESTAMP NOT NULL,
                        total_amount DECIMAL(15, 2) NOT NULL,
                        FOREIGN KEY (customer_id) REFERENCES customers(id)
                    );
                """;

        String createOrderItemTable = """
                    CREATE TABLE IF NOT EXISTS order_items (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        order_id BIGINT NOT NULL,
                        product_id BIGINT NOT NULL,
                        quantity INT NOT NULL,
                        unit_price DECIMAL(15, 2) NOT NULL,
                        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                        FOREIGN KEY (product_id) REFERENCES products(id)
                    );
                """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createProductTable);
            stmt.execute(createCustomerTable);
            stmt.execute(createOrderTable);
            stmt.execute(createOrderItemTable);
            System.out.println("[DB] Database schema initialized successfully.");
        } catch (SQLException e) {
            System.err.println("[DB Error] Failed to initialize database: " + e.getMessage());
        }
    }

}