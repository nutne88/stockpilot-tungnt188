package vn.edu.fpt.model;

import vn.edu.fpt.exception.InvalidInputException;

public class Customer {
    private Long id;
    private String name;
    private String email;
    private String phone;

    public Customer() {}

    public Customer(Long id, String name, String email, String phone) {
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        String phoneRegex = "^\\d{10,11}$"; 

        if (email == null || !email.matches(emailRegex)) {
            throw new InvalidInputException("Invalid email format! Got: " + email);
        }
        if (phone == null || !phone.matches(phoneRegex)) {
            throw new InvalidInputException("Invalid phone format! Must be 10-11 digits. Got: " + phone);
        }

        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    // Getters và Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    public String toString() {
        return String.format("CustomerID: %d | Name: %s | Email: %s | Phone: %s", id, name, email, phone);
    }
}