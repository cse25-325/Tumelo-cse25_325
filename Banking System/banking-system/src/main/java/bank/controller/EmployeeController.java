package bank.controller;

import bank.dao.AccountDAO;
import bank.dao.CustomerDAO;
import bank.dao.TransactionDAO;
import bank.dao.TransactionRunner;
import bank.model.Account;
import bank.model.BankEmployee;
import bank.model.BankException.AccessDeniedException;
import bank.model.Customer;
import bank.model.Role;
import bank.model.Transaction;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Back-office actions that only a bank employee may perform. */
public class EmployeeController {

    /** Outcome of a monthly interest run. */
    public static final class InterestSummary {
        private final int accountsCredited;
        private final int accountsSkipped;
        private final BigDecimal totalInterest;

        InterestSummary(int accountsCredited, int accountsSkipped, BigDecimal totalInterest) {
            this.accountsCredited = accountsCredited;
            this.accountsSkipped = accountsSkipped;
            this.totalInterest = totalInterest;
        }

        public int getAccountsCredited() { return accountsCredited; }
        public int getAccountsSkipped() { return accountsSkipped; }
        public BigDecimal getTotalInterest() { return totalInterest; }
    }

    private final CustomerDAO customers;
    private final AccountDAO accounts;
    private final TransactionDAO transactions;
    private final TransactionRunner tx;
    private final BankEmployee employee = new BankEmployee();

    public EmployeeController(CustomerDAO customers, AccountDAO accounts, TransactionDAO transactions,
                              TransactionRunner tx) {
        this.customers = customers;
        this.accounts = accounts;
        this.transactions = transactions;
        this.tx = tx;
    }

    /** All customers with their accounts loaded. */
    public List<Customer> listCustomers(Session session) {
        requireEmployee(session);
        List<Customer> all = customers.findAll();
        all.forEach(c -> accounts.findByCustomer(c).forEach(c::addAccount));
        return all;
    }

    public List<Account> listAccounts(Session session) {
        requireEmployee(session);
        return accounts.findAll();
    }

    public List<Transaction> getHistory(Session session, String accountNumber) {
        requireEmployee(session);
        return transactions.findByAccount(accountNumber);
    }

    /**
     * Credits this month's interest to every interest-bearing account.
     * An account that already received interest this month is skipped, so a double click cannot pay twice.
     */
    public InterestSummary creditMonthlyInterest(Session session) {
        return creditMonthlyInterest(session, YearMonth.now());
    }

    InterestSummary creditMonthlyInterest(Session session, YearMonth month) {
        requireEmployee(session);
        return tx.run(() -> {
            int credited = 0;
            int skipped = 0;
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (Account account : accounts.findAll()) {
                if (transactions.hasInterestInMonth(account.getAccountNumber(), month)) {
                    skipped++;
                    continue;
                }
                Transaction t = employee.creditMonthlyInterest(account);
                if (t == null) {
                    continue; // e.g. Cheque account, or zero balance
                }
                accounts.update(account);
                transactions.create(t);
                credited++;
                total = total.add(t.getAmount());
            }
            return new InterestSummary(credited, skipped, total);
        });
    }

    private void requireEmployee(Session session) {
        if (session == null || session.getRole() != Role.EMPLOYEE) {
            throw new AccessDeniedException("Only bank employees can do that.");
        }
    }
}
