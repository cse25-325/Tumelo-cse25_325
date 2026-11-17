package bank.model;

import java.math.BigDecimal;

/**
 * Cheque account (salary account). Deposits and withdrawals of any size up to the
 * balance; only for people who work, so employer details are mandatory. Pays no interest.
 */
public class ChequeAccount extends Account implements Withdrawal {

    private final String companyName;
    private final String companyAddress;

    public ChequeAccount(String accountNumber, String branch, Customer owner, BigDecimal balance,
                         String companyName, String companyAddress) {
        super(accountNumber, branch, owner, balance);
        this.companyName = companyName;
        this.companyAddress = companyAddress;
    }

    /** True when both employer details are present. */
    public boolean validateEmployment() {
        return isPresent(companyName) && isPresent(companyAddress);
    }

    /** Rule check used when a NEW cheque account is opened. */
    public static void validateEmployment(String companyName, String companyAddress) {
        if (!isPresent(companyName) || !isPresent(companyAddress)) {
            throw new BankException.ValidationException(
                    "A Cheque account needs your employer's company name and company address.");
        }
    }

    private static boolean isPresent(String s) {
        return s != null && !s.trim().isEmpty();
    }

    @Override
    public Transaction withdraw(BigDecimal amount) {
        return debit(amount, "Withdrawal");
    }

    @Override
    public BigDecimal calculateInterest() {
        return BigDecimal.ZERO.setScale(2); // cheque accounts earn no interest
    }

    @Override
    public BigDecimal getInterestRate() {
        return BigDecimal.ZERO;
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.CHEQUE;
    }

    public String getCompanyName() { return companyName; }
    public String getCompanyAddress() { return companyAddress; }
}
