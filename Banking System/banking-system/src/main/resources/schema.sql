-- Banking System schema (H2). Mirrors the class diagram:
--   Customer 1 --- * Account (composition: no account without a customer)
--   Account  1 --- * Transaction
--   LoginRecord (users) -> optional Customer
CREATE TABLE IF NOT EXISTS customers (
    customer_id  VARCHAR(10)  PRIMARY KEY,
    first_name   VARCHAR(50)  NOT NULL,
    surname      VARCHAR(50)  NOT NULL,
    address      VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    username      VARCHAR(30)  PRIMARY KEY,
    password_hash VARCHAR(128) NOT NULL,
    salt          VARCHAR(64)  NOT NULL,
    role          VARCHAR(10)  NOT NULL,
    customer_id   VARCHAR(10)  REFERENCES customers(customer_id),
    CONSTRAINT chk_role CHECK (role IN ('CUSTOMER', 'EMPLOYEE'))
);

CREATE TABLE IF NOT EXISTS accounts (
    account_number  VARCHAR(15)   PRIMARY KEY,
    customer_id     VARCHAR(10)   NOT NULL REFERENCES customers(customer_id),
    account_type    VARCHAR(12)   NOT NULL,
    branch          VARCHAR(50)   NOT NULL,
    balance         DECIMAL(15,2) NOT NULL,
    company_name    VARCHAR(100),
    company_address VARCHAR(200),
    opened_at       TIMESTAMP     NOT NULL,
    CONSTRAINT chk_account_type CHECK (account_type IN ('SAVINGS', 'INVESTMENT', 'CHEQUE')),
    CONSTRAINT chk_balance CHECK (balance >= 0)
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(15)   NOT NULL REFERENCES accounts(account_number),
    txn_type       VARCHAR(10)   NOT NULL,
    amount         DECIMAL(15,2) NOT NULL,
    balance_after  DECIMAL(15,2) NOT NULL,
    description    VARCHAR(200),
    txn_time       TIMESTAMP     NOT NULL,
    CONSTRAINT chk_txn_type CHECK (txn_type IN ('DEPOSIT', 'WITHDRAWAL', 'INTEREST'))
);
