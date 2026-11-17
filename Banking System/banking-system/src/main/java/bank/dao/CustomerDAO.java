package bank.dao;

import bank.model.Customer;

import java.util.List;
import java.util.Optional;

/** CRUD for customers. Returned customers do NOT have their accounts loaded. */
public interface CustomerDAO {
    String nextId();
    void create(Customer customer);
    Optional<Customer> findById(String customerId);
    List<Customer> findAll();
    void update(Customer customer);
    void delete(String customerId);
}
