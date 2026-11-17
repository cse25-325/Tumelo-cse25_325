package bank.model;

import java.math.BigDecimal;

/**
 * Capability interface: only accounts that implement this may be withdrawn from.
 * SavingsAccount deliberately does NOT implement it, so the rule
 * "no withdrawals from Savings" is enforced by the type system as well as at runtime.
 */
public interface Withdrawal {

    /**
     * Takes money out of the account.
     *
     * @return the transaction that was recorded
     * @throws BankException.InsufficientFundsException if the balance is too low
     */
    Transaction withdraw(BigDecimal amount);
}
