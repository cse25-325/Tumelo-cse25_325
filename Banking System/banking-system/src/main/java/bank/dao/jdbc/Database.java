package bank.dao.jdbc;

import bank.dao.TransactionRunner;
import bank.model.BankException.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

/**
 * Owns the single JDBC connection, creates the schema from {@code schema.sql},
 * and provides all-or-nothing units of work for the DAOs.
 */
public class Database implements TransactionRunner, AutoCloseable {

    private final Connection connection;
    private boolean inTransaction;

    public Database(String jdbcUrl) {
        try {
            this.connection = DriverManager.getConnection(jdbcUrl, "sa", "");
            createSchema();
        } catch (SQLException e) {
            throw new DataAccessException("Could not open the database: " + e.getMessage(), e);
        }
    }

    public Connection connection() {
        return connection;
    }

    private void createSchema() throws SQLException {
        String script;
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new SQLException("schema.sql not found on the classpath");
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("Could not read schema.sql", e);
        }
        try (Statement st = connection.createStatement()) {
            for (String sql : script.split(";")) {
                String cleaned = stripComments(sql).trim();
                if (!cleaned.isEmpty()) {
                    st.execute(cleaned);
                }
            }
        }
    }

    private static String stripComments(String sql) {
        StringBuilder sb = new StringBuilder();
        for (String line : sql.split("\\R")) {
            if (!line.trim().startsWith("--")) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public synchronized <T> T run(Supplier<T> work) {
        if (inTransaction) { // already inside a unit of work: join it
            return work.get();
        }
        try {
            connection.setAutoCommit(false);
            inTransaction = true;
            T result = work.get();
            connection.commit();
            return result;
        } catch (RuntimeException e) {
            rollbackQuietly();
            throw e;
        } catch (SQLException e) {
            rollbackQuietly();
            throw new DataAccessException("The transaction failed: " + e.getMessage(), e);
        } finally {
            inTransaction = false;
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
                // connection is unusable anyway
            }
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // nothing more we can do
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // closing on exit
        }
    }
}
