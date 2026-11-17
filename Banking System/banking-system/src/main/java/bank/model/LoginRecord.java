package bank.model;

/** Stored credentials. Passwords are never kept in plain text: only salted PBKDF2 hashes. */
public final class LoginRecord {

    private final String username;
    private final String passwordHash;
    private final String salt;
    private final Role role;
    private final String customerId; // null for employees

    public LoginRecord(String username, String passwordHash, String salt, Role role, String customerId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        this.customerId = customerId;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }
    public Role getRole() { return role; }
    public String getCustomerId() { return customerId; }
}
