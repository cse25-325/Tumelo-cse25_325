package bank.testsupport;

import bank.controller.BankingController;
import bank.dao.AccountDAO;
import bank.dao.CustomerDAO;
import bank.dao.TransactionDAO;
import bank.dao.UserDAO;
import bank.model.Account;
import bank.model.Customer;
import bank.model.LoginRecord;
import bank.model.Transaction;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Map-backed DAOs so controller logic can be tested without a database. */
public final class InMemoryDaos {

    private InMemoryDaos() {
    }

    public static BankingController newBank() {
        Customers c = new Customers();
        return new BankingController(c, new Accounts(), new Transactions(), new Users(), tx(), () -> { });
    }

    private static bank.dao.TransactionRunner tx() {
        return new bank.dao.TransactionRunner() {
            @Override
            public <T> T run(java.util.function.Supplier<T> work) {
                return work.get();
            }
        };
    }

    public static class Customers implements CustomerDAO {
        private final Map<String, Customer> map = new LinkedHashMap<>();
        public String nextId() { return String.format("C%04d", map.size() + 1); }
        public void create(Customer c) { map.put(c.getCustomerId(), c); }
        public Optional<Customer> findById(String id) { return Optional.ofNullable(map.get(id)); }
        public List<Customer> findAll() { return new ArrayList<>(map.values()); }
        public void update(Customer c) { map.put(c.getCustomerId(), c); }
        public void delete(String id) { map.remove(id); }
    }

    public static class Accounts implements AccountDAO {
        private final Map<String, Account> map = new LinkedHashMap<>();
        public String nextAccountNumber() { return String.valueOf(1000001 + map.size()); }
        public void create(Account a) { map.put(a.getAccountNumber(), a); }
        public Optional<Account> findByNumber(String n) { return Optional.ofNullable(map.get(n)); }
        public List<Account> findByCustomer(Customer o) {
            List<Account> list = new ArrayList<>();
            for (Account a : map.values()) {
                if (a.getOwner().getCustomerId().equals(o.getCustomerId())) list.add(a);
            }
            return list;
        }
        public List<Account> findAll() { return new ArrayList<>(map.values()); }
        public void update(Account a) { map.put(a.getAccountNumber(), a); }
        public void delete(String n) { map.remove(n); }
    }

    public static class Transactions implements TransactionDAO {
        private final List<Transaction> all = new ArrayList<>();
        public void create(Transaction t) { all.add(t); }
        public List<Transaction> findByAccount(String n) {
            List<Transaction> list = new ArrayList<>();
            for (Transaction t : all) if (t.getAccountNumber().equals(n)) list.add(0, t);
            return list;
        }
        public boolean hasInterestInMonth(String n, YearMonth m) {
            for (Transaction t : all) {
                if (t.getAccountNumber().equals(n) && t.getType() == bank.model.TransactionType.INTEREST
                        && YearMonth.from(t.getTimestamp()).equals(m)) return true;
            }
            return false;
        }
    }

    public static class Users implements UserDAO {
        private final Map<String, LoginRecord> map = new LinkedHashMap<>();
        public void save(LoginRecord r) { map.put(r.getUsername(), r); }
        public Optional<LoginRecord> findByUsername(String u) { return Optional.ofNullable(map.get(u)); }
    }
}
