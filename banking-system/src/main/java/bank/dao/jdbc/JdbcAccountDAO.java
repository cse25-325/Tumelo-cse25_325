package bank.dao.jdbc;

import bank.dao.AccountDAO;
import bank.dao.CustomerDAO;
import bank.model.Account;
import bank.model.AccountType;
import bank.model.BankException.DataAccessException;
import bank.model.ChequeAccount;
import bank.model.Customer;
import bank.model.InvestmentAccount;
import bank.model.SavingsAccount;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcAccountDAO implements AccountDAO {

    private static final String COLUMNS =
            "account_number, customer_id, account_type, branch, balance, company_name, company_address";

    private final Database db;
    private final CustomerDAO customers;

    public JdbcAccountDAO(Database db, CustomerDAO customers) {
        this.db = db;
        this.customers = customers;
    }

    @Override
    public String nextAccountNumber() {
        String sql = "SELECT COALESCE(MAX(CAST(account_number AS BIGINT)), 1000000) + 1 FROM accounts";
        try (Statement st = db.connection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return String.valueOf(rs.getLong(1));
        } catch (SQLException e) {
            throw new DataAccessException("Could not generate an account number: " + e.getMessage(), e);
        }
    }

    @Override
    public void create(Account a) {
        String sql = "INSERT INTO accounts (" + COLUMNS + ", opened_at) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, a.getAccountNumber());
            ps.setString(2, a.getOwner().getCustomerId());
            ps.setString(3, a.getAccountType().name());
            ps.setString(4, a.getBranch());
            ps.setBigDecimal(5, a.getBalance());
            if (a instanceof ChequeAccount) {
                ps.setString(6, ((ChequeAccount) a).getCompanyName());
                ps.setString(7, ((ChequeAccount) a).getCompanyAddress());
            } else {
                ps.setNull(6, java.sql.Types.VARCHAR);
                ps.setNull(7, java.sql.Types.VARCHAR);
            }
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the account: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Account> findByNumber(String accountNumber) {
        String sql = "SELECT " + COLUMNS + " FROM accounts WHERE account_number = ?";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Customer owner = customers.findById(rs.getString("customer_id")).orElseThrow(
                        () -> new DataAccessException("Account has no owner in the database.", null));
                return Optional.of(map(rs, owner));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the account: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Account> findByCustomer(Customer owner) {
        String sql = "SELECT " + COLUMNS + " FROM accounts WHERE customer_id = ? ORDER BY account_number";
        List<Account> list = new ArrayList<>();
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, owner.getCustomerId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs, owner));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list the accounts: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Account> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM accounts ORDER BY account_number";
        List<Account> list = new ArrayList<>();
        Map<String, Customer> owners = new HashMap<>();
        try (Statement st = db.connection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("customer_id");
                Customer owner = owners.computeIfAbsent(id, k -> customers.findById(k).orElseThrow(
                        () -> new DataAccessException("Account has no owner in the database.", null)));
                list.add(map(rs, owner));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list the accounts: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Account a) {
        try (PreparedStatement ps = db.connection()
                .prepareStatement("UPDATE accounts SET balance = ? WHERE account_number = ?")) {
            ps.setBigDecimal(1, a.getBalance());
            ps.setString(2, a.getAccountNumber());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the account: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String accountNumber) {
        try (PreparedStatement t = db.connection().prepareStatement("DELETE FROM transactions WHERE account_number = ?");
             PreparedStatement a = db.connection().prepareStatement("DELETE FROM accounts WHERE account_number = ?")) {
            t.setString(1, accountNumber);
            t.executeUpdate();
            a.setString(1, accountNumber);
            a.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the account: " + e.getMessage(), e);
        }
    }

    /** Builds the right subclass for the stored type. */
    private static Account map(ResultSet rs, Customer owner) throws SQLException {
        AccountType type = AccountType.valueOf(rs.getString("account_type"));
        String number = rs.getString("account_number");
        String branch = rs.getString("branch");
        BigDecimal balance = rs.getBigDecimal("balance");
        Account account;
        switch (type) {
            case SAVINGS:
                account = new SavingsAccount(number, branch, owner, balance);
                break;
            case INVESTMENT:
                account = new InvestmentAccount(number, branch, owner, balance);
                break;
            default:
                account = new ChequeAccount(number, branch, owner, balance,
                        rs.getString("company_name"), rs.getString("company_address"));
        }
        return account;
    }
}
