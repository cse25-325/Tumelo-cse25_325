package bank.model;

/**
 * Base class for every business-rule failure in the system.
 * The message is always written for the end user, so the GUI can show it as-is
 * instead of crashing (non-functional requirement: friendly error messages).
 */
public class BankException extends RuntimeException {

    public BankException(String message) {
        super(message);
    }

    public BankException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Input did not pass validation (blank name, bad amount, weak password...). */
    public static class ValidationException extends BankException {
        public ValidationException(String message) {
            super(message);
        }
    }

    /** Withdrawal larger than the available balance. */
    public static class InsufficientFundsException extends BankException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }

    /** Operation blocked by an account rule (e.g. withdrawing from Savings). */
    public static class RuleViolationException extends BankException {
        public RuleViolationException(String message) {
            super(message);
        }
    }

    /** Bad login, or an attempt to act outside the user's role / ownership. */
    public static class AccessDeniedException extends BankException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }

    /** Persistence layer failure. */
    public static class DataAccessException extends BankException {
        public DataAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
