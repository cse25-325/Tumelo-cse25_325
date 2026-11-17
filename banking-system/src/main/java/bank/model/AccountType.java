package bank.model;

/** The three kinds of account the bank offers. */
public enum AccountType {
    SAVINGS("Savings"),
    INVESTMENT("Investment"),
    CHEQUE("Cheque");

    private final String label;

    AccountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
