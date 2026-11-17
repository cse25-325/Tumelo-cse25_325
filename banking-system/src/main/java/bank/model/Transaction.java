package bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable record of one balance change on an account. */
public final class Transaction {

    private final Long id; // null until stored in the database
    private final String accountNumber;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String description;
    private final LocalDateTime timestamp;

    /** New transaction, timestamped now. */
    public Transaction(String accountNumber, TransactionType type, BigDecimal amount,
                       BigDecimal balanceAfter, String description) {
        this(null, accountNumber, type, amount, balanceAfter, description, LocalDateTime.now());
    }

    /** Re-creates a stored transaction. */
    public Transaction(Long id, String accountNumber, TransactionType type, BigDecimal amount,
                       BigDecimal balanceAfter, String description, LocalDateTime timestamp) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return timestamp + " " + type + " " + amount + " (balance " + balanceAfter + ")";
    }
}
