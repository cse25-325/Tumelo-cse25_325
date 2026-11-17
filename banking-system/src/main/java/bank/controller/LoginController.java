package bank.controller;

import bank.dao.CustomerDAO;
import bank.dao.UserDAO;
import bank.model.BankException.AccessDeniedException;
import bank.model.BankException.ValidationException;
import bank.model.Customer;
import bank.model.LoginRecord;
import bank.model.Role;
import bank.util.PasswordHasher;

/** Authenticates users and ends sessions. */
public class LoginController {

    private static final String BAD_LOGIN = "Incorrect username or password. Please try again.";

    private final UserDAO users;
    private final CustomerDAO customers;

    public LoginController(UserDAO users, CustomerDAO customers) {
        this.users = users;
        this.customers = customers;
    }

    public Session login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new ValidationException("Please enter both your username and password.");
        }
        LoginRecord record = users.findByUsername(username.trim())
                .orElseThrow(() -> new AccessDeniedException(BAD_LOGIN));
        if (!PasswordHasher.verify(password, record.getSalt(), record.getPasswordHash())) {
            throw new AccessDeniedException(BAD_LOGIN);
        }
        if (record.getRole() == Role.EMPLOYEE) {
            return new Session(record.getUsername(), Role.EMPLOYEE, null, "Bank Employee");
        }
        Customer customer = customers.findById(record.getCustomerId())
                .orElseThrow(() -> new AccessDeniedException(BAD_LOGIN));
        return new Session(record.getUsername(), Role.CUSTOMER, customer.getCustomerId(), customer.getFullName());
    }

    /** Nothing is kept server-side; the caller simply drops the Session. */
    public void logout(Session session) {
        // hook for audit logging in future
    }
}
