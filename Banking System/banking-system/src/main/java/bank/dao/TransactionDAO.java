package bank.dao;

import bank.model.Transaction;

import java.time.YearMonth;
import java.util.List;

public interface TransactionDAO {
    void create(Transaction transaction);
    /** Newest first. */
    List<Transaction> findByAccount(String accountNumber);
    boolean hasInterestInMonth(String accountNumber, YearMonth month);
}
