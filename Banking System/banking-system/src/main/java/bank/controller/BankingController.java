package bank.controller;

import bank.dao.AccountDAO;
import bank.dao.CustomerDAO;
import bank.dao.TransactionDAO;
import bank.dao.TransactionRunner;
import bank.dao.UserDAO;
import bank.dao.jdbc.Database;
import bank.dao.jdbc.JdbcAccountDAO;
import bank.dao.jdbc.JdbcCustomerDAO;
import bank.dao.jdbc.JdbcTransactionDAO;
import bank.dao.jdbc.JdbcUserDAO;
import bank.model.LoginRecord;
import bank.model.Role;
import bank.util.PasswordHasher;

/**
 * Top-level controller and composition root: builds the DAOs and the specialised controllers
 * and wires them together. The GUI only ever talks to the controllers exposed here.
 */
public class BankingController implements AutoCloseable {

    public static final String DEFAULT_EMPLOYEE_USERNAME = "employee";
    public static final String DEFAULT_EMPLOYEE_PASSWORD = "Employee123";

    private final AutoCloseable resource;
    private final CustomerDAO customerDAO;
    private final UserDAO userDAO;
    private final LoginController loginController;
    private final CustomerController customerController;
    private final AccountController accountController;
    private final EmployeeController employeeController;

    /** Wires the controllers around any DAO implementation (JDBC in production, in-memory in unit tests). */
    public BankingController(CustomerDAO customerDAO, AccountDAO accountDAO, TransactionDAO transactionDAO,
                             UserDAO userDAO, TransactionRunner tx, AutoCloseable resource) {
        this.resource = resource;
        this.customerDAO = customerDAO;
        this.userDAO = userDAO;
        this.loginController = new LoginController(userDAO, customerDAO);
        this.customerController = new CustomerController(customerDAO, accountDAO, userDAO, tx);
        this.accountController = new AccountController(customerDAO, accountDAO, transactionDAO, tx);
        this.employeeController = new EmployeeController(customerDAO, accountDAO, transactionDAO, tx);
        ensureDefaultEmployee();
    }

    /** Opens (or creates) the database at the given JDBC URL and wires everything on top of it. */
    public static BankingController create(String jdbcUrl) {
        Database db = new Database(jdbcUrl);
        CustomerDAO customers = new JdbcCustomerDAO(db);
        AccountDAO accounts = new JdbcAccountDAO(db, customers);
        return new BankingController(customers, accounts, new JdbcTransactionDAO(db), new JdbcUserDAO(db), db, db);
    }

    private void ensureDefaultEmployee() {
        if (userDAO.findByUsername(DEFAULT_EMPLOYEE_USERNAME).isEmpty()) {
            String salt = PasswordHasher.newSalt();
            userDAO.save(new LoginRecord(DEFAULT_EMPLOYEE_USERNAME,
                    PasswordHasher.hash(DEFAULT_EMPLOYEE_PASSWORD, salt), salt, Role.EMPLOYEE, null));
        }
    }

    public boolean hasCustomers() {
        return !customerDAO.findAll().isEmpty();
    }

    public LoginController login() { return loginController; }
    public CustomerController customers() { return customerController; }
    public AccountController accounts() { return accountController; }
    public EmployeeController employee() { return employeeController; }

    @Override
    public void close() {
        try {
            resource.close();
        } catch (Exception ignored) {
            // shutting down
        }
    }
}
