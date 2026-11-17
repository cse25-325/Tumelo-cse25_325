package bank.dao.jdbc;

import bank.dao.UserDAO;
import bank.model.BankException.DataAccessException;
import bank.model.LoginRecord;
import bank.model.Role;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcUserDAO implements UserDAO {

    private final Database db;

    public JdbcUserDAO(Database db) {
        this.db = db;
    }

    @Override
    public void save(LoginRecord r) {
        String sql = "INSERT INTO users (username, password_hash, salt, role, customer_id) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, r.getUsername());
            ps.setString(2, r.getPasswordHash());
            ps.setString(3, r.getSalt());
            ps.setString(4, r.getRole().name());
            ps.setString(5, r.getCustomerId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the login: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<LoginRecord> findByUsername(String username) {
        String sql = "SELECT username, password_hash, salt, role, customer_id FROM users WHERE username = ?";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new LoginRecord(rs.getString("username"), rs.getString("password_hash"),
                        rs.getString("salt"), Role.valueOf(rs.getString("role")), rs.getString("customer_id")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the login: " + e.getMessage(), e);
        }
    }
}
