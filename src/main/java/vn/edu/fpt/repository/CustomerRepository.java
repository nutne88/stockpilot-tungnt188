package vn.edu.fpt.repository;

import vn.edu.fpt.model.Customer;
import vn.edu.fpt.util.DatabaseConnection;
import vn.edu.fpt.exception.DataAccessException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerRepository implements Repository<Customer, Long> {

    @Override
    public void save(Customer customer) {
        String sql = "INSERT INTO customers (name, email, phone) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, customer.getName());
            pstmt.setString(2, customer.getEmail());
            pstmt.setString(3, customer.getPhone());

            pstmt.executeUpdate();
            System.out.println("[Repo] Customer saved successfully to database.");
        } catch (SQLException e) {
            throw new DataAccessException("Error saving customer to database", e);
        }
    }

    @Override
    public List<Customer> findAll() {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Customer customer = new Customer(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"));
                customers.add(customer);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error fetching customer list", e);
        }
        return customers;
    }
}