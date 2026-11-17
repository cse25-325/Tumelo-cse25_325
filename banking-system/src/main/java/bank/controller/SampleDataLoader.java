package bank.controller;

import bank.model.AccountType;
import bank.model.Customer;

/**
 * Inserts 10 sample customers (with accounts and transactions) so the system can be demonstrated.
 * Every sample customer logs in with username cust01..cust10 and password {@value #PASSWORD}.
 */
public final class SampleDataLoader {

    public static final String PASSWORD = "Password1";

    private static final String[][] PEOPLE = {
            {"Thabo", "Molefe", "Plot 112, Block 8, Gaborone"},
            {"Neo", "Kgosi", "Plot 45, Phase 2, Francistown"},
            {"Lerato", "Sebego", "Plot 7, Boseja, Maun"},
            {"Kabelo", "Mothibi", "Plot 301, Extension 9, Gaborone"},
            {"Boitumelo", "Ditshwanelo", "Plot 88, Lobatse Road, Kanye"},
            {"Onalenna", "Tau", "Plot 19, Serowe Main Road, Palapye"},
            {"Tshepo", "Maruping", "Plot 530, Tlokweng"},
            {"Mpho", "Setlhare", "Plot 64, Area D, Selebi-Phikwe"},
            {"Naledi", "Phiri", "Plot 23, Gaborone West"},
            {"Keabetswe", "Modise", "Plot 9, Letlhakane Village"}
    };

    private SampleDataLoader() {
    }

    public static void load(BankingController bank) {
        for (int i = 0; i < PEOPLE.length; i++) {
            String username = String.format("cust%02d", i + 1);
            Customer c = bank.customers().register(PEOPLE[i][0], PEOPLE[i][1], PEOPLE[i][2], username, PASSWORD);
            String id = c.getCustomerId();
            String branch = "Gaborone Main";

            // everyone has a savings account
            String savings = bank.accounts().openAccount(id, AccountType.SAVINGS, "1000", branch, null, null)
                    .getAccountNumber();
            bank.accounts().deposit(id, savings, String.valueOf(250 + i * 100));

            if (i % 2 == 0) { // half also hold an investment account
                String inv = bank.accounts().openAccount(id, AccountType.INVESTMENT, String.valueOf(500 + i * 250),
                        branch, null, null).getAccountNumber();
                bank.accounts().withdraw(id, inv, "50");
            }
            if (i % 3 == 0) { // a third are employed and hold a cheque account
                String chq = bank.accounts().openAccount(id, AccountType.CHEQUE, "5000", branch,
                        "Sample Holdings (Pty) Ltd", "Plot 1, Main Mall, Gaborone").getAccountNumber();
                bank.accounts().withdraw(id, chq, "300");
            }
        }
    }
}
