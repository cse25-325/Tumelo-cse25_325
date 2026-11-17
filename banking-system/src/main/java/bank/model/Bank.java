package bank.model;

import java.util.List;

/** Static facts about the bank itself. */
public final class Bank {

    public static final String NAME = "Community Bank";
    public static final String CURRENCY = "BWP";
    public static final List<String> BRANCHES = List.of("Gaborone Main", "Francistown", "Maun", "Palapye");

    private Bank() {
    }
}
