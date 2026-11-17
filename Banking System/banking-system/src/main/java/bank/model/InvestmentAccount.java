package bank.model;

import java.math.BigDecimal;

/** Investment account: 5% monthly interest, withdrawals allowed, BWP 500.00 minimum to open. */
public class InvestmentAccount extends Account implements Withdrawal {

    public static final BigDecimal INTEREST_RATE = new BigDecimal("0.05"); // 5%
    public static final BigDecimal MINIMUM_OPENING_DEPOSIT = new BigDecimal("500.00");

    public InvestmentAccount(String accountNumber, String branch, Customer owner, BigDecimal balance) {
        super(accountNumber, branch, owner, balance);
    }

    /** Rule check used when a NEW investment account is opened. */
    public static void validateOpeningDeposit(BigDecimal initialDeposit) {
        if (initialDeposit == null || initialDeposit.compareTo(MINIMUM_OPENING_DEPOSIT) < 0) {
            throw new BankException.ValidationException(
                    "An Investment account needs an opening deposit of at least BWP 500.00.");
        }
    }

    @Override
    public Transaction withdraw(BigDecimal amount) {
        return debit(amount, "Withdrawal");
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
        return AccountType.INVESTMENT;
    }
}
