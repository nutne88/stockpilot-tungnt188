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

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createProductTable);
            System.out.println("Intialize DB successfully.");
        } catch (SQLException e) {
            System.err.println("DB Error : " + e.getMessage());
        }
    }
}