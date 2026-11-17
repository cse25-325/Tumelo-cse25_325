package bank.controller;

import bank.dao.AccountDAO;
import bank.dao.CustomerDAO;
import bank.dao.TransactionDAO;
import bank.dao.TransactionRunner;
import bank.model.Account;
import bank.model.AccountType;
import bank.model.BankEmployee;
import bank.model.BankException.AccessDeniedException;
import bank.model.BankException.ValidationException;
import bank.model.Customer;
import bank.model.Transaction;
import bank.model.Withdrawal;
import bank.util.Money;

import java.math.BigDecimal;
import java.util.List;

/**
 * Handles a customer's account requests: parses and validates the raw text from the GUI,
 * calls the domain model, and saves the result. A customer can only touch their own accounts.
 */
public class AccountController {

    private final CustomerDAO customers;
    private final AccountDAO accounts;
    private final TransactionDAO transactions;
    private final TransactionRunner tx;
    private final BankEmployee employee = new BankEmployee();

    public AccountController(CustomerDAO customers, AccountDAO accounts, TransactionDAO transactions,
                             TransactionRunner tx) {
        this.customers = customers;
        this.accounts = accounts;
        this.transactions = transactions;
        this.tx = tx;
    }

    public List<Account> getAccounts(String customerId) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new ValidationException("Customer not found."));
        return accounts.findByCustomer(customer);
    }

    public Account openAccount(String customerId, AccountType type, String initialDepositText, String branch,
                               String companyName, String companyAddress) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new ValidationException("Customer not found."));
        BigDecimal deposit = Money.parseOptional(initialDepositText);
        return tx.run(() -> {
            BankEmployee.OpenedAccount opened = employee.openAccount(type, accounts.nextAccountNumber(), branch,
                    customer, deposit, companyName, companyAddress);
            accounts.create(opened.getAccount());
            if (opened.getOpeningDeposit() != null) {
                transactions.create(opened.getOpeningDeposit());
            }
            return opened.getAccount();
        });
    }

    public Transaction deposit(String customerId, String accountNumber, String amountText) {
        BigDecimal amount = Money.parsePositive(amountText);
        return tx.run(() -> {
            Account account = owned(customerId, accountNumber);
            Transaction t = account.deposit(amount);
            accounts.update(account);
            transactions.create(t);
            return t;
        });
    }

    public Transaction withdraw(String customerId, String accountNumber, String amountText) {
        BigDecimal amount = Money.parsePositive(amountText);
        return tx.run(() -> {
            Account account = owned(customerId, accountNumber);
            Withdrawal withdrawable = employee.requireWithdrawable(account); // blocks Savings
            Transaction t = withdrawable.withdraw(amount);
            accounts.update(account);
            transactions.create(t);
            return t;
        });
    }

    public BigDecimal getBalance(String customerId, String accountNumber) {
        return owned(customerId, accountNumber).getBalance();
    }

    /** Newest first. */
    public List<Transaction> getHistory(String customerId, String accountNumber) {
        owned(customerId, accountNumber);
        return transactions.findByAccount(accountNumber);
    }

    private Account owned(String customerId, String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new ValidationException("Please select an account first.");
        }
        Account account = accounts.findByNumber(accountNumber)
                .orElseThrow(() -> new AccessDeniedException("Account not found."));
        if (!account.getOwner().getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("Account not found."); // same message: don't reveal other accounts
        }
        return account;
    }
}
