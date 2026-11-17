package bank.dao.jdbc;

import bank.dao.TransactionDAO;
import bank.model.BankException.DataAccessException;
import bank.model.Transaction;
import bank.model.TransactionType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class JdbcTransactionDAO implements TransactionDAO {

    private final Database db;

    public JdbcTransactionDAO(Database db) {
        this.db = db;
    }

    @Override
    public void create(Transaction t) {
        String sql = "INSERT INTO transactions (account_number, txn_type, amount, balance_after, description, txn_time)"
                + " VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, t.getAccountNumber());
            ps.setString(2, t.getType().name());
            ps.setBigDecimal(3, t.getAmount());
            ps.setBigDecimal(4, t.getBalanceAfter());
            ps.setString(5, t.getDescription());
            ps.setTimestamp(6, Timestamp.valueOf(t.getTimestamp()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> findByAccount(String accountNumber) {
        String sql = "SELECT transaction_id, account_number, txn_type, amount, balance_after, description, txn_time "
                + "FROM transactions WHERE account_number = ? ORDER BY txn_time DESC, transaction_id DESC";
        List<Transaction> list = new ArrayList<>();
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Transaction(rs.getLong("transaction_id"), rs.getString("account_number"),
                            TransactionType.valueOf(rs.getString("txn_type")), rs.getBigDecimal("amount"),
                            rs.getBigDecimal("balance_after"), rs.getString("description"),
                            rs.getTimestamp("txn_time").toLocalDateTime()));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the transactions: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasInterestInMonth(String accountNumber, YearMonth month) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE account_number = ? AND txn_type = 'INTEREST' "
                + "AND txn_time >= ? AND txn_time < ?";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setTimestamp(2, Timestamp.valueOf(month.atDay(1).atStartOfDay()));
            ps.setTimestamp(3, Timestamp.valueOf(month.plusMonths(1).atDay(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check interest history: " + e.getMessage(), e);
        }
    }
}
