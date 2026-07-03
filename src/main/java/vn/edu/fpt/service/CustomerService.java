package vn.edu.fpt.service;

import vn.edu.fpt.exception.InvalidInputException;
import vn.edu.fpt.model.Customer;
import vn.edu.fpt.repository.CustomerRepository;

import java.util.List;

public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer createCustomer(String name, String email, String phone) {
        Customer customer = new Customer(null, name, email, phone);
        customerRepository.save(customer);
        return customer;
    }

    public List<Customer> listAll() {
        return customerRepository.findAll();
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new InvalidInputException("No customer found with id: " + id));
    }
}