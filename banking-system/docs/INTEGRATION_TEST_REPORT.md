# Integration Test Report (Task 8)

Run with `mvn test`. Fill the **Actual result** column from `target/surefire-reports/` after running.

| # | Test (class `BankingIntegrationTest` unless noted) | Flow covered | Expected | Actual result |
|---|---|---|---|---|
| 1 | `fullCustomerJourneyIsPersisted` | register → login → open Savings/Investment/Cheque → deposit → withdraw → savings withdrawal blocked → employee credits interest → re-read from DB via a second controller | Balances 1500.75 / 1050.00 / 1249.50; savings history has 3 rows, newest is INTEREST | _run & record_ |
| 2 | `ruleFailuresRollBackAndLeaveNoPartialData` | Investment opened with BWP 100 | Rejected; no account row saved | _run & record_ |
| 3 | `passwordsAreNotStoredInPlainText` | register then query `users` table | Stored value ≠ plain password | _run & record_ |
| 4 | `customerCannotReadAnotherCustomersAccount` | customer B reads customer A's account | AccessDeniedException | _run & record_ |
| 5 | `tenSampleRecordsAreLoaded` | sample data loader | 10 customers, ≥ 10 accounts | _run & record_ |
| 6 | `AccountRulesTest` (12 tests) | domain rules: min deposit, employment, no savings withdrawal, interest rates, validation | all pass | 12/12 passed (verified in development, no DB needed) |
| 7 | `ControllerFlowTest` (11 tests) | controller logic on in-memory DAOs incl. interest once per month, ownership, role checks | all pass | 11/11 passed (verified in development) |
