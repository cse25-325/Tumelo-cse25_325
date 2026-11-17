package bank.model;

import bank.model.BankException.RuleViolationException;
import bank.model.BankException.ValidationException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * The bank employee's responsibilities, expressed as pure business rules (no I/O):
 * validating registrations, enforcing account-specific rules and crediting interest.
 * Controllers call this class and then hand the results to the DAOs to be saved.
 */
public class BankEmployee {

    private static final Pattern NAME = Pattern.compile("^[\\p{L}][\\p{L} '\\-]{0,48}[\\p{L}]$|^\\p{L}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{4,20}$");

    /** Result of opening an account: the account plus its opening-deposit transaction (may be null). */
    public static final class OpenedAccount {
        private final Account account;
        private final Transaction openingDeposit;

        OpenedAccount(Account account, Transaction openingDeposit) {
            this.account = account;
            this.openingDeposit = openingDeposit;
        }

        public Account getAccount() { return account; }
        public Transaction getOpeningDeposit() { return openingDeposit; }
    }

    // ---------------- registration ----------------

    public void validateRegistration(String firstName, String surname, String address,
                                     String username, String password) {
        if (!isName(firstName)) {
            throw new ValidationException("Please enter a valid first name (letters only).");
        }
        if (!isName(surname)) {
            throw new ValidationException("Please enter a valid surname (letters only).");
        }
        if (address == null || address.trim().length() < 5) {
            throw new ValidationException("Please enter your full address (at least 5 characters).");
        }
        if (username == null || !USERNAME.matcher(username.trim()).matches()) {
            throw new ValidationException("Username must be 4-20 characters: letters, digits or underscore.");
        }
        validatePassword(password);
    }

    public void validatePassword(String password) {
        boolean ok = password != null && password.length() >= 8
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
        if (!ok) {
            throw new ValidationException("Password must be at least 8 characters and include a letter and a digit.");
        }
    }

    private static boolean isName(String s) {
        return s != null && NAME.matcher(s.trim()).matches();
    }

    // ---------------- account opening ----------------

    /**
     * Applies the opening rules for each account type and returns the new account.
     * Savings: any non-negative opening deposit. Investment: at least BWP 500.00.
     * Cheque: employer name and address are mandatory.
     */
    public OpenedAccount openAccount(AccountType type, String accountNumber, String branch, Customer owner,
                                     BigDecimal initialDeposit, String companyName, String companyAddress) {
        if (type == null) {
            throw new ValidationException("Please choose an account type.");
        }
        if (owner == null) {
            throw new ValidationException("An account must be held by a customer.");
        }
        if (branch == null || branch.trim().isEmpty()) {
            throw new ValidationException("Please choose a branch.");
        }
        BigDecimal deposit = initialDeposit == null ? BigDecimal.ZERO : initialDeposit;
        if (deposit.signum() < 0) {
            throw new ValidationException("The opening deposit cannot be negative.");
        }

        Account account;
        switch (type) {
            case SAVINGS:
                account = new SavingsAccount(accountNumber, branch, owner, BigDecimal.ZERO);
                break;
            case INVESTMENT:
                InvestmentAccount.validateOpeningDeposit(deposit);
                account = new InvestmentAccount(accountNumber, branch, owner, BigDecimal.ZERO);
                break;
            case CHEQUE:
                ChequeAccount.validateEmployment(companyName, companyAddress);
                account = new ChequeAccount(accountNumber, branch, owner, BigDecimal.ZERO,
                        companyName.trim(), companyAddress.trim());
                break;
            default:
                throw new ValidationException("Unknown account type.");
        }
        Transaction opening = deposit.signum() > 0 ? account.deposit(deposit, "Opening deposit") : null;
        return new OpenedAccount(account, opening);
    }

    // ---------------- transaction rules ----------------

    /** Blocks withdrawals on accounts that do not support them (Savings). */
    public Withdrawal requireWithdrawable(Account account) {
        if (account instanceof Withdrawal) {
            return (Withdrawal) account;
        }
        throw new RuleViolationException(
                "Withdrawals are not allowed on a " + account.getAccountType().getLabel() + " account.");
    }

    // ---------------- interest ----------------

    /** Credits one month of interest; returns null when the account earns nothing. */
    public Transaction creditMonthlyInterest(Account account) {
        return account.creditInterest();
    }
}
