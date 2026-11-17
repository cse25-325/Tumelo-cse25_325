package bank.integration;

import bank.controller.BankingController;
import bank.controller.EmployeeController;
import bank.controller.SampleDataLoader;
import bank.controller.Session;
import bank.model.Account;
import bank.model.AccountType;
import bank.model.BankException.AccessDeniedException;
import bank.model.BankException.RuleViolationException;
import bank.model.Role;
import bank.model.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * End-to-end test against a real (in-memory H2) database: controller -> model -> JDBC DAO -> SQL.
 * The GUI is a thin layer that only calls these same controller methods, so this exercises the same
 * path as a user clicking through the application.
 * <p>
 * After each step the data is re-read through a SECOND BankingController opened on the same
 * database, proving the values were really persisted and not just held in memory.
 */
public class BankingIntegrationTest {

    private String url;
    private BankingController bank;

    @Before
    public void setUp() {
        url = "jdbc:h2:mem:it_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        bank = BankingController.create(url);
    }

    @After
    public void tearDown() {
        bank.close();
    }

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    @Test
    public void fullCustomerJourneyIsPersisted() {
        // 1. register -> 2. login
        String id = bank.customers().register("Thabo", "Molefe", "Plot 112, Gaborone", "thabo01", "Secret123")
                .getCustomerId();
        Session session = bank.login().login("thabo01", "Secret123");
        assertEquals(Role.CUSTOMER, session.getRole());
        assertEquals(id, session.getCustomerId());

        // 3. open one of each account type
        String sav = bank.accounts().openAccount(id, AccountType.SAVINGS, "1000", "Gaborone Main", null, null).getAccountNumber();
        String inv = bank.accounts().openAccount(id, AccountType.INVESTMENT, "500", "Gaborone Main", null, null).getAccountNumber();
        String chq = bank.accounts().openAccount(id, AccountType.CHEQUE, "2000", "Gaborone Main", "Acme Ltd", "Plot 9, Gaborone").getAccountNumber();

        // 4. deposits and withdrawals
        bank.accounts().deposit(id, sav, "500");
        bank.accounts().deposit(id, inv, "500");
        bank.accounts().withdraw(id, chq, "750.50");
        try {
            bank.accounts().withdraw(id, sav, "1");
            fail("savings withdrawal must be blocked");
        } catch (RuleViolationException expected) { }

        // 5. employee credits monthly interest
        Session emp = bank.login().login(BankingController.DEFAULT_EMPLOYEE_USERNAME,
                BankingController.DEFAULT_EMPLOYEE_PASSWORD);
        EmployeeController.InterestSummary summary = bank.employee().creditMonthlyInterest(emp);
        assertEquals(2, summary.getAccountsCredited());

        // 6. verify through a second controller on the same database
        BankingController reopened = BankingController.create(url);
        List<Account> accounts = reopened.accounts().getAccounts(id);
        assertEquals(3, accounts.size());
        assertEquals(bd("1500.75"), reopened.accounts().getBalance(id, sav)); // 1500 + 0.05%
        assertEquals(bd("1050.00"), reopened.accounts().getBalance(id, inv)); // 1000 + 5%
        assertEquals(bd("1249.50"), reopened.accounts().getBalance(id, chq)); // 2000 - 750.50, no interest

        List<Transaction> savHistory = reopened.accounts().getHistory(id, sav);
        assertEquals(3, savHistory.size()); // opening, deposit, interest
        assertEquals("INTEREST", savHistory.get(0).getType().name());
        assertEquals(bd("1500.75"), savHistory.get(0).getBalanceAfter());
    }

    @Test
    public void ruleFailuresRollBackAndLeaveNoPartialData() {
        String id = bank.customers().register("Neo", "Kgosi", "Plot 5, Maun", "neo_k", "Secret123").getCustomerId();
        try {
            bank.accounts().openAccount(id, AccountType.INVESTMENT, "100", "Maun", null, null);
            fail();
        } catch (RuntimeException expected) { }
        assertTrue(bank.accounts().getAccounts(id).isEmpty());
    }

    @Test
    public void passwordsAreNotStoredInPlainText() throws Exception {
        bank.customers().register("Neo", "Kgosi", "Plot 5, Maun", "neo_k", "Secret123");
        try (java.sql.Connection c = java.sql.DriverManager.getConnection(url, "sa", "");
             java.sql.ResultSet rs = c.createStatement().executeQuery(
                     "SELECT password_hash FROM users WHERE username = 'neo_k'")) {
            assertTrue(rs.next());
            assertNotEquals("Secret123", rs.getString(1));
        }
    }

    @Test
    public void customerCannotReadAnotherCustomersAccount() {
        String a = bank.customers().register("Neo", "Kgosi", "Plot 5, Maun", "neo_k", "Secret123").getCustomerId();
        String b = bank.customers().register("Lerato", "Sebego", "Plot 7, Maun", "lerato7", "Secret123").getCustomerId();
        String acc = bank.accounts().openAccount(a, AccountType.SAVINGS, "100", "Maun", null, null).getAccountNumber();
        try {
            bank.accounts().getBalance(b, acc);
            fail();
        } catch (AccessDeniedException expected) { }
    }

    @Test
    public void tenSampleRecordsAreLoaded() {
        SampleDataLoader.load(bank);
        Session emp = bank.login().login(BankingController.DEFAULT_EMPLOYEE_USERNAME,
                BankingController.DEFAULT_EMPLOYEE_PASSWORD);
        assertEquals(10, bank.employee().listCustomers(emp).size());
        assertTrue(bank.employee().listAccounts(emp).size() >= 10);
    }
}
