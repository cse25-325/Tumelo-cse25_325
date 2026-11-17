package bank.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A person who banks with us. A customer may hold several accounts. */
public class Customer {

    private final String customerId;
    private String firstName;
    private String surname;
    private String address;
    private final List<Account> accounts = new ArrayList<>(); // encapsulated: exposed read-only

    public Customer(String customerId, String firstName, String surname, String address) {
        this.customerId = customerId;
        this.firstName = firstName;
        this.surname = surname;
        this.address = address;
    }

    public void addAccount(Account account) {
        if (account == null) {
            throw new BankException.ValidationException("Account is required.");
        }
        if (!accounts.contains(account)) {
            accounts.add(account);
        }
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public String getCustomerId() { return customerId; }
    public String getFirstName() { return firstName; }
    public String getSurname() { return surname; }
    public String getAddress() { return address; }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setSurname(String surname) { this.surname = surname; }
    public void setAddress(String address) { this.address = address; }

    public String getFullName() {
        return firstName + " " + surname;
    }

    @Override
    public String toString() {
        return customerId + " - " + getFullName();
    }
}
