package bank.util;

import bank.model.BankException.ValidationException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

/** Parsing and formatting of money amounts (BWP, two decimals). */
public final class Money {

    private static final Pattern AMOUNT = Pattern.compile("^\\d{1,12}(\\.\\d{1,2})?$");

    private Money() {
    }

    /** Parses text typed by a user, e.g. "1250" or "1250.50". Must be greater than zero. */
    public static BigDecimal parsePositive(String text) {
        BigDecimal value = parseNonNegative(text);
        if (value.signum() <= 0) {
            throw new ValidationException("The amount must be greater than zero.");
        }
        return value;
    }

    /** Like {@link #parsePositive} but a blank entry means zero. */
    public static BigDecimal parseOptional(String text) {
        if (text == null || text.trim().isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        return parseNonNegative(text);
    }

    private static BigDecimal parseNonNegative(String text) {
        String cleaned = text == null ? "" : text.trim().replace(",", "");
        if (!AMOUNT.matcher(cleaned).matches()) {
            throw new ValidationException("Please enter a valid amount using digits only, e.g. 250 or 250.50.");
        }
        return new BigDecimal(cleaned).setScale(2);
    }

    public static String format(BigDecimal amount) {
        return "BWP " + new DecimalFormat("#,##0.00").format(amount);
    }
}
