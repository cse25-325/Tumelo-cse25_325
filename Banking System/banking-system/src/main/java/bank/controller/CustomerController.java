package bank.controller;

import bank.dao.AccountDAO;
import bank.dao.CustomerDAO;
import bank.dao.TransactionRunner;
import bank.dao.UserDAO;
import bank.model.BankEmployee;
import bank.model.BankException.ValidationException;
import bank.model.Customer;
import bank.model.LoginRecord;
import bank.model.Role;
import bank.util.PasswordHasher;

/** Registers new customers (the bank employee validates, then the DAOs save). */
public class CustomerController {

    private final CustomerDAO customers;
    private final AccountDAO accounts;
    private final UserDAO users;
    private final TransactionRunner tx;
    private final BankEmployee employee = new BankEmployee();

    public CustomerController(CustomerDAO customers, AccountDAO accounts, UserDAO users, TransactionRunner tx) {
        this.customers = customers;
        this.accounts = accounts;
        this.users = users;
        this.tx = tx;
    }

    public Customer register(String firstName, String surname, String address, String username, String password) {
        employee.validateRegistration(firstName, surname, address, username, password);
        String user = username.trim();
        if (users.findByUsername(user).isPresent()) {
            throw new ValidationException("That username is already taken. Please choose another one.");
        }
        return tx.run(() -> {
            Customer customer = new Customer(customers.nextId(), firstName.trim(), surname.trim(), address.trim());
            customers.create(customer);
            String salt = PasswordHasher.newSalt();
            users.save(new LoginRecord(user, PasswordHasher.hash(password, salt), salt, Role.CUSTOMER,
                    customer.getCustomerId()));
            return customer;
        });
    }

    /** Loads a customer together with all their accounts. */
    public Customer getCustomer(String customerId) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new ValidationException("Customer not found."));
        accounts.findByCustomer(customer).forEach(customer::addAccount);
        return customer;
    }
}
