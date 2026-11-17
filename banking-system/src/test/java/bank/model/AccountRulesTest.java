package bank.model;

import bank.model.BankException.InsufficientFundsException;
import bank.model.BankException.RuleViolationException;
import bank.model.BankException.ValidationException;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class AccountRulesTest {

    private Customer customer;
    private final BankEmployee employee = new BankEmployee();

    @Before
    public void setUp() {
        customer = new Customer("C0001", "Thabo", "Molefe", "Plot 1, Gaborone");
    }

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    @Test(expected = ValidationException.class)
    public void accountCannotExistWithoutCustomer() {
        new SavingsAccount("1", "Main", null, BigDecimal.ZERO);
    }

    @Test
    public void depositIncreasesBalanceAndRecordsTransaction() {
        Account a = new SavingsAccount("1", "Main", customer, BigDecimal.ZERO);
        Transaction t = a.deposit(bd("100.50"));
        assertEquals(bd("100.50"), a.getBalance());
        assertEquals(TransactionType.DEPOSIT, t.getType());
        assertEquals(bd("100.50"), t.getBalanceAfter());
    }

    @Test
    public void overloadedDepositUsesCustomDescription() {
        Account a = new SavingsAccount("1", "Main", customer, BigDecimal.ZERO);
        assertEquals("Salary", a.deposit(bd("10"), "Salary").getDescription());
        assertEquals("Deposit", a.deposit(bd("10")).getDescription());
    }

    @Test(expected = ValidationException.class)
    public void zeroOrNegativeDepositRejected() {
        new SavingsAccount("1", "Main", customer, BigDecimal.ZERO).deposit(bd("-5"));
    }

    @Test
    public void savingsAccountIsNotWithdrawable() {
        Account a = new SavingsAccount("1", "Main", customer, bd("1000"));
        assertFalse(a instanceof Withdrawal);
        try {
            employee.requireWithdrawable(a);
            fail("expected rule violation");
        } catch (RuleViolationException e) {
            assertTrue(e.getMessage().contains("not allowed"));
        }
    }

    @Test
    public void investmentAndChequeAllowWithdrawal() {
        InvestmentAccount inv = new InvestmentAccount("2", "Main", customer, bd("800"));
        ChequeAccount chq = new ChequeAccount("3", "Main", customer, bd("800"), "Acme", "Gaborone");
        assertEquals(bd("700.00"), balanceAfter(inv, "100"));
        assertEquals(bd("700.00"), balanceAfter(chq, "100"));
    }

    private static BigDecimal balanceAfter(Withdrawal w, String amount) {
        return w.withdraw(new BigDecimal(amount)).getBalanceAfter();
    }

    @Test(expected = InsufficientFundsException.class)
    public void cannotWithdrawMoreThanBalance() {
        new InvestmentAccount("2", "Main", customer, bd("100")).withdraw(bd("100.01"));
    }

    @Test
    public void investmentNeeds500ToOpen() {
        try {
            employee.openAccount(AccountType.INVESTMENT, "2", "Main", customer, bd("499.99"), null, null);
            fail("expected validation failure");
        } catch (ValidationException expected) {
            assertTrue(expected.getMessage().contains("500.00"));
        }
        BankEmployee.OpenedAccount ok = employee.openAccount(AccountType.INVESTMENT, "2", "Main", customer,
                bd("500.00"), null, null);
        assertEquals(bd("500.00"), ok.getAccount().getBalance());
        assertNotNull(ok.getOpeningDeposit());
    }

    @Test
    public void chequeNeedsEmploymentDetails() {
        for (String[] bad : new String[][]{{null, null}, {"Acme", ""}, {" ", "Gaborone"}}) {
            try {
                employee.openAccount(AccountType.CHEQUE, "3", "Main", customer, bd("0"), bad[0], bad[1]);
                fail("expected validation failure");
            } catch (ValidationException expected) {
                // good
            }
        }
        Account a = employee.openAccount(AccountType.CHEQUE, "3", "Main", customer, bd("0"), "Acme", "Gaborone")
                .getAccount();
        assertTrue(((ChequeAccount) a).validateEmployment());
    }

    @Test
    public void monthlyInterestRates() {
        assertEquals(bd("0.50"), new SavingsAccount("1", "M", customer, bd("1000")).calculateInterest());   // 0.05%
        assertEquals(bd("50.00"), new InvestmentAccount("2", "M", customer, bd("1000")).calculateInterest()); // 5%
        assertEquals(0, new ChequeAccount("3", "M", customer, bd("1000"), "A", "B").calculateInterest().signum());
    }

    @Test
    public void creditInterestAddsToBalanceAndRecordsTransaction() {
        InvestmentAccount inv = new InvestmentAccount("2", "M", customer, bd("1000"));
        Transaction t = inv.creditInterest();
        assertEquals(TransactionType.INTEREST, t.getType());
        assertEquals(bd("1050.00"), inv.getBalance());
        assertNull(new ChequeAccount("3", "M", customer, bd("1000"), "A", "B").creditInterest());
    }

    @Test
    public void registrationValidation() {
        employee.validateRegistration("Neo", "Kgosi", "Plot 5, Maun", "neo_k", "Passw0rdX");
        String[][] bad = {
                {"", "Kgosi", "Plot 5, Maun", "neo_k", "Passw0rdX"},
                {"Neo", "K0si", "Plot 5, Maun", "neo_k", "Passw0rdX"},
                {"Neo", "Kgosi", "Maun", "neo_k", "Passw0rdX"},
                {"Neo", "Kgosi", "Plot 5, Maun", "n", "Passw0rdX"},
                {"Neo", "Kgosi", "Plot 5, Maun", "neo_k", "short1"},
                {"Neo", "Kgosi", "Plot 5, Maun", "neo_k", "allletters"},
        };
        for (String[] b : bad) {
            try {
                employee.validateRegistration(b[0], b[1], b[2], b[3], b[4]);
                fail("expected validation failure for " + String.join("|", b));
            } catch (ValidationException expected) {
                // good
            }
        }
    }
}
