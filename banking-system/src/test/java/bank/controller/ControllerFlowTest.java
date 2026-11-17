package bank.controller;

import bank.model.*;
import bank.model.BankException.AccessDeniedException;
import bank.model.BankException.RuleViolationException;
import bank.model.BankException.ValidationException;
import bank.testsupport.InMemoryDaos;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.Assert.*;

/** Controller + model logic against in-memory DAOs (no database needed). */
public class ControllerFlowTest {

    private BankingController bank;
    private String custId;

    @Before
    public void setUp() {
        bank = InMemoryDaos.newBank();
        custId = bank.customers().register("Thabo", "Molefe", "Plot 112, Gaborone", "thabo01", "Secret123")
                .getCustomerId();
    }

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    @Test
    public void loginSucceedsAndFailsCorrectly() {
        Session s = bank.login().login("thabo01", "Secret123");
        assertEquals(Role.CUSTOMER, s.getRole());
        assertEquals(custId, s.getCustomerId());
        try { bank.login().login("thabo01", "wrong"); fail(); } catch (AccessDeniedException expected) { }
        try { bank.login().login("nobody", "Secret123"); fail(); } catch (AccessDeniedException expected) { }
        try { bank.login().login("", ""); fail(); } catch (ValidationException expected) { }
        assertEquals(Role.EMPLOYEE, bank.login().login("employee", BankingController.DEFAULT_EMPLOYEE_PASSWORD).getRole());
    }

    @Test(expected = ValidationException.class)
    public void duplicateUsernameRejected() {
        bank.customers().register("Neo", "Kgosi", "Plot 5, Maun", "thabo01", "Secret123");
    }

    @Test
    public void customerCanHoldAllThreeAccountTypes() {
        bank.accounts().openAccount(custId, AccountType.SAVINGS, "", "Maun", null, null);
        bank.accounts().openAccount(custId, AccountType.INVESTMENT, "500", "Maun", null, null);
        bank.accounts().openAccount(custId, AccountType.CHEQUE, "", "Maun", "Acme", "Plot 2");
        assertEquals(3, bank.accounts().getAccounts(custId).size());
        assertEquals(3, bank.customers().getCustomer(custId).getAccounts().size());
    }

    @Test
    public void depositAndWithdrawUpdateBalanceAndHistory() {
        String acc = bank.accounts().openAccount(custId, AccountType.CHEQUE, "100", "Maun", "Acme", "Plot 2")
                .getAccountNumber();
        bank.accounts().deposit(custId, acc, "400.25");
        bank.accounts().withdraw(custId, acc, "50");
        assertEquals(bd("450.25"), bank.accounts().getBalance(custId, acc));
        List<Transaction> history = bank.accounts().getHistory(custId, acc);
        assertEquals(3, history.size());
        assertEquals(TransactionType.WITHDRAWAL, history.get(0).getType()); // newest first
    }

    @Test
    public void savingsWithdrawalIsBlockedAndBalanceUntouched() {
        String acc = bank.accounts().openAccount(custId, AccountType.SAVINGS, "1000", "Maun", null, null)
                .getAccountNumber();
        try {
            bank.accounts().withdraw(custId, acc, "10");
            fail("withdrawal from savings must be blocked");
        } catch (RuleViolationException expected) { }
        assertEquals(bd("1000.00"), bank.accounts().getBalance(custId, acc));
    }

    @Test
    public void badAmountsGiveFriendlyErrors() {
        String acc = bank.accounts().openAccount(custId, AccountType.SAVINGS, "", "Maun", null, null).getAccountNumber();
        for (String bad : new String[]{"", "abc", "-5", "0", "10.999", "1e5"}) {
            try { bank.accounts().deposit(custId, acc, bad); fail("accepted: " + bad); }
            catch (ValidationException expected) { }
        }
    }

    @Test
    public void investmentBelowMinimumRejectedAndNothingSaved() {
        try {
            bank.accounts().openAccount(custId, AccountType.INVESTMENT, "499", "Maun", null, null);
            fail();
        } catch (ValidationException expected) { }
        assertTrue(bank.accounts().getAccounts(custId).isEmpty());
    }

    @Test
    public void customerCannotUseAnotherCustomersAccount() {
        String acc = bank.accounts().openAccount(custId, AccountType.SAVINGS, "100", "Maun", null, null).getAccountNumber();
        String other = bank.customers().register("Neo", "Kgosi", "Plot 5, Maun", "neo_k", "Secret123").getCustomerId();
        try { bank.accounts().deposit(other, acc, "10"); fail(); } catch (AccessDeniedException expected) { }
        try { bank.accounts().getHistory(other, acc); fail(); } catch (AccessDeniedException expected) { }
    }

    @Test
    public void monthlyInterestIsCreditedOncePerMonth() {
        String sav = bank.accounts().openAccount(custId, AccountType.SAVINGS, "1000", "Maun", null, null).getAccountNumber();
        String inv = bank.accounts().openAccount(custId, AccountType.INVESTMENT, "1000", "Maun", null, null).getAccountNumber();
        bank.accounts().openAccount(custId, AccountType.CHEQUE, "1000", "Maun", "Acme", "Plot 2");
        Session emp = bank.login().login("employee", BankingController.DEFAULT_EMPLOYEE_PASSWORD);

        EmployeeController.InterestSummary first = bank.employee().creditMonthlyInterest(emp);
        assertEquals(2, first.getAccountsCredited());
        assertEquals(bd("50.50"), first.getTotalInterest());
        assertEquals(bd("1000.50"), bank.accounts().getBalance(custId, sav));
        assertEquals(bd("1050.00"), bank.accounts().getBalance(custId, inv));

        EmployeeController.InterestSummary second = bank.employee().creditMonthlyInterest(emp);
        assertEquals(0, second.getAccountsCredited());
        assertEquals(2, second.getAccountsSkipped());
        assertEquals(bd("1050.00"), bank.accounts().getBalance(custId, inv));

        // a different month is paid again
        assertEquals(2, bank.employee().creditMonthlyInterest(emp, YearMonth.now().plusMonths(1)).getAccountsCredited());
    }

    @Test
    public void onlyEmployeesCanRunEmployeeActions() {
        Session cust = bank.login().login("thabo01", "Secret123");
        try { bank.employee().creditMonthlyInterest(cust); fail(); } catch (AccessDeniedException expected) { }
        try { bank.employee().listCustomers(cust); fail(); } catch (AccessDeniedException expected) { }
    }

    @Test
    public void sampleDataLoadsTenCustomers() {
        BankingController fresh = InMemoryDaos.newBank();
        SampleDataLoader.load(fresh);
        Session emp = fresh.login().login("employee", BankingController.DEFAULT_EMPLOYEE_PASSWORD);
        assertEquals(10, fresh.employee().listCustomers(emp).size());
        assertEquals(Role.CUSTOMER, fresh.login().login("cust10", SampleDataLoader.PASSWORD).getRole());
    }
}
