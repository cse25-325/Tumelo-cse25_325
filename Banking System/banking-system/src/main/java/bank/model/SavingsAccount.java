package bank.model;

import java.math.BigDecimal;

/**
 * Savings account: pays 0.05% monthly interest and NEVER allows withdrawals.
 * It does not implement {@link Withdrawal}, so a withdrawal cannot even be attempted.
 */
public class SavingsAccount extends Account {

    public static final BigDecimal INTEREST_RATE = new BigDecimal("0.0005"); // 0.05%

    public SavingsAccount(String accountNumber, String branch, Customer owner, BigDecimal balance) {
        super(accountNumber, branch, owner, balance);
    }

    @Override
    public BigDecimal calculateInterest() {
        return interestOnBalance();
    }

    @Override
    public BigDecimal getInterestRate() {
        return INTEREST_RATE;
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.SAVINGS;
    }
}
