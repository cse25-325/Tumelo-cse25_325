package bank.dao;

import java.util.function.Supplier;

/** Runs several DAO calls as one all-or-nothing unit of work. */
public interface TransactionRunner {
    <T> T run(Supplier<T> work);
}
