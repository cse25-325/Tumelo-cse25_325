package bank.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Abstract parent of every account type (abstraction + inheritance).
 * <p>
 * An account cannot exist without an owner: the constructor rejects a null
 * {@link Customer}. Balance is private (encapsulation) and only changes
 * through {@code deposit}, {@code debit} and {@code creditInterest}.
 */
public abstract class Account {

    private final String accountNumber;
    private final String branch;
    private final Customer owner;
    private BigDecimal balance;

    protected Account(String accountNumber, String branch, Customer owner, BigDecimal balance) {
        if (owner == null) {
            throw new BankException.ValidationException("An account must be held by a customer.");
        }
        this.accountNumber = accountNumber;
        this.branch = branch;
        this.owner = owner;
        this.balance = balance == null ? BigDecimal.ZERO.setScale(2) : balance.setScale(2, RoundingMode.HALF_UP);
    }

    // ---- polymorphic behaviour each account type must define ----

    /** Interest earned for one month on the current balance. */
    public abstract BigDecimal calculateInterest();

    /** Monthly interest rate as a fraction (0.05 = 5%). */
    public abstract BigDecimal getInterestRate();

    public abstract AccountType getAccountType();

    // ---- shared behaviour ----

    /** Deposit with the default description (overload 1). */
    public Transaction deposit(BigDecimal amount) {
        return deposit(amount, "Deposit");
    }

    /** Deposit with a custom description (overload 2). */
    public Transaction deposit(BigDecimal amount, String description) {
        BigDecimal value = requirePositive(amount);
        balance = balance.add(value);
        return new Transaction(accountNumber, TransactionType.DEPOSIT, value, balance, description);
    }

    /**
     * Credits one month of interest. Template method: the amount comes from the
     * subclass's {@link #calculateInterest()}. Returns {@code null} when there is no interest to pay
     * (e.g. Cheque accounts or an empty balance).
     */
    public Transaction creditInterest() {
        BigDecimal interest = calculateInterest();
        if (interest.signum() <= 0) {
            return null;
        }
        balance = balance.add(interest);
        return new Transaction(accountNumber, TransactionType.INTEREST, interest, balance,
                "Monthly interest @ " + getInterestRate().multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%");
    }

    /** Used by subclasses that implement {@link Withdrawal}. */
    protected Transaction debit(BigDecimal amount, String description) {
        BigDecimal value = requirePositive(amount);
        if (value.compareTo(balance) > 0) {
            throw new BankException.InsufficientFundsException(
                    "Insufficient funds: you tried to withdraw BWP " + value + " but the balance is BWP " + balance + ".");
        }
        balance = balance.subtract(value);
        return new Transaction(accountNumber, TransactionType.WITHDRAWAL, value, balance, description);
    }

    private static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BankException.ValidationException("The amount must be greater than zero.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /** Interest helper: balance x rate, rounded to the nearest thebe. */
    protected BigDecimal interestOnBalance() {
        return balance.multiply(getInterestRate()).setScale(2, RoundingMode.HALF_UP);
    }

    public String getAccountNumber() { return accountNumber; }
    public String getBranch() { return branch; }
    public Customer getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }

    @Override
    public String toString() {
        return getAccountType() + " account " + accountNumber + " (BWP " + balance + ")";
    }
}
