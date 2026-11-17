# Banking System — CSE202 OOAD with Java (Part B)

A JavaFX desktop banking application with an embedded H2 database, built to the functional and
non-functional requirements and the class diagram in your documents.

## Run it (GitHub Codespaces or locally)

Requires JDK 17+ and Maven.

```bash
mvn test            # unit + integration tests
mvn javafx:run      # start the application
```

In Codespaces the `.devcontainer` adds a desktop: open the forwarded port **6080** (noVNC, password `vscode`)
and the JavaFX window appears there. The database file is created in `./data/` on first run and
**10 sample customers are inserted automatically** when the database is empty (assignment guideline 4).

### Logins

| Who | Username | Password |
|---|---|---|
| Bank employee | `employee` | `Employee123` |
| Sample customers | `cust01` … `cust10` | `Password1` |
| New customer | register on the login screen | your choice (8+ chars, letter + digit) |

## Architecture (assignment weeks 4–8)

```
view  (JavaFX boundary classes)      LoginView, RegisterView, CustomerView, EmployeeView, BankApp
  │  only calls ↓                    no business logic, no DAO/SQL
controller                           BankingController, LoginController, CustomerController,
  │  validates + orchestrates ↓      AccountController, EmployeeController, Session
model  (domain)                      Account*, Customer, BankEmployee, Transaction, Withdrawal, ...
  │
dao (interfaces) + dao.jdbc (JDBC)   CustomerDAO, AccountDAO, TransactionDAO, UserDAO → H2
```

* `src/main/resources/schema.sql` – database schema (customers, users, accounts, transactions).
* Controllers depend on **DAO interfaces**, so the same logic is tested with in-memory DAOs and run with JDBC.
* Money is `BigDecimal` throughout; passwords are salted PBKDF2 hashes.

## OOP principles demonstrated (class-diagram marks)

| Principle | Where |
|---|---|
| Abstraction | `abstract class Account` (`calculateInterest()`, `getInterestRate()`, `getAccountType()` abstract) |
| Inheritance | `SavingsAccount`, `InvestmentAccount`, `ChequeAccount` extend `Account` |
| Interface | `Withdrawal` – implemented by Investment and Cheque, **not** Savings, so Savings can never be withdrawn from |
| Polymorphism | `account.calculateInterest()` / `creditInterest()` behave per account type; DAOs and controllers handle `Account` generically |
| Overriding | `calculateInterest`, `getInterestRate`, `getAccountType`, `withdraw`, `toString` |
| Overloading | `Account.deposit(amount)` and `Account.deposit(amount, description)` |
| Encapsulation | private fields, balance changes only via `deposit`/`debit`/`creditInterest`, read-only `Customer.getAccounts()` |
| Composition | an `Account` requires a non-null owner `Customer`; `Customer` holds its accounts |

## Requirement traceability

| Requirement | Implemented in |
|---|---|
| Register with first name, surname, address; stored in DB | `CustomerController.register`, `JdbcCustomerDAO` |
| Open one or many Savings / Investment / Cheque accounts | `AccountController.openAccount`, `BankEmployee.openAccount` |
| Cheque needs employer name + address | `ChequeAccount.validateEmployment` |
| Deposit to any account; withdraw from Investment/Cheque | `AccountController.deposit/withdraw`, `Withdrawal` |
| View balance and transaction history | `AccountController.getBalance/getHistory`, History tab |
| Employee validates + saves registration | `BankEmployee.validateRegistration` → DAOs |
| Balances updated automatically for every transaction | account change + transaction row saved in one DB transaction |
| Block Savings withdrawals; BWP 500.00 Investment minimum | `BankEmployee.requireWithdrawable`, `InvestmentAccount.validateOpeningDeposit` |
| Monthly interest: Savings 0.05 %, Investment 5 % | `EmployeeController.creditMonthlyInterest` (once per account per month) |
| Friendly errors, confirmations, clear menus | `Ui.guard` dialogs, tabbed screens |

## Notes / deliberate choices

* **Interest** follows the brief literally: 0.05 % and 5 % *per month* on the current balance. Cheque accounts pay none.
* **Cheque withdrawals** are "any amount" up to the available balance (no overdraft).
* The class diagram's `BankingDatabase` is realised as the four DAO interfaces + JDBC classes, and `Bank` holds bank-wide constants.
* Integration tests exercise controller → model → JDBC → H2 (the GUI only forwards to those same controller calls).
  See `docs/INTEGRATION_TEST_REPORT.md`.
