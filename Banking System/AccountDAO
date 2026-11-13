package bank.dao;

import bank.model.Account;
import bank.model.Customer;

import java.util.List;
import java.util.Optional;

/** CRUD for accounts of every type. */
public interface AccountDAO {
    String nextAccountNumber();
    void create(Account account);
    Optional<Account> findByNumber(String accountNumber);
    /** Accounts held by the given customer (their owner reference is that customer). */
    List<Account> findByCustomer(Customer owner);
    List<Account> findAll();
    /** Saves the current balance. */
    void update(Account account);
    void delete(String accountNumber);
}
