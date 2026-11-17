package bank.dao.jdbc;

import bank.dao.CustomerDAO;
import bank.model.BankException.DataAccessException;
import bank.model.BankException.ValidationException;
import bank.model.Customer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCustomerDAO implements CustomerDAO {

    private final Database db;

    public JdbcCustomerDAO(Database db) {
        this.db = db;
    }

    @Override
    public String nextId() {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(customer_id, 2) AS INT)), 0) + 1 FROM customers";
        try (Statement st = db.connection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return String.format("C%04d", rs.getInt(1));
        } catch (SQLException e) {
            throw new DataAccessException("Could not generate a customer id: " + e.getMessage(), e);
        }
    }

    @Override
    public void create(Customer c) {
        String sql = "INSERT INTO customers (customer_id, first_name, surname, address) VALUES (?,?,?,?)";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, c.getCustomerId());
            ps.setString(2, c.getFirstName());
            ps.setString(3, c.getSurname());
            ps.setString(4, c.getAddress());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the customer: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        String sql = "SELECT customer_id, first_name, surname, address FROM customers WHERE customer_id = ?";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the customer: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Customer> findAll() {
        String sql = "SELECT customer_id, first_name, surname, address FROM customers ORDER BY customer_id";
        List<Customer> list = new ArrayList<>();
        try (Statement st = db.connection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list customers: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Customer c) {
        String sql = "UPDATE customers SET first_name = ?, surname = ?, address = ? WHERE customer_id = ?";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, c.getFirstName());
            ps.setString(2, c.getSurname());
            ps.setString(3, c.getAddress());
            ps.setString(4, c.getCustomerId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the customer: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String customerId) {
        try (PreparedStatement count = db.connection()
                .prepareStatement("SELECT COUNT(*) FROM accounts WHERE customer_id = ?")) {
            count.setString(1, customerId);
            try (ResultSet rs = count.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    throw new ValidationException("A customer who still holds accounts cannot be deleted.");
                }
            }
            try (PreparedStatement del = db.connection().prepareStatement("DELETE FROM users WHERE customer_id = ?")) {
                del.setString(1, customerId);
                del.executeUpdate();
            }
            try (PreparedStatement del = db.connection().prepareStatement("DELETE FROM customers WHERE customer_id = ?")) {
                del.setString(1, customerId);
                del.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the customer: " + e.getMessage(), e);
        }
    }

    private static Customer map(ResultSet rs) throws SQLException {
        return new Customer(rs.getString("customer_id"), rs.getString("first_name"),
                rs.getString("surname"), rs.getString("address"));
    }
}
