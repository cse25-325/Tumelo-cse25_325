package bank.controller;

import bank.model.Role;

/** Who is logged in. Created by {@link LoginController}, passed to the other controllers. */
public final class Session {

    private final String username;
    private final Role role;
    private final String customerId; // null for employees
    private final String displayName;

    public Session(String username, Role role, String customerId, String displayName) {
        this.username = username;
        this.role = role;
        this.customerId = customerId;
        this.displayName = displayName;
    }

    public String getUsername() { return username; }
    public Role getRole() { return role; }
    public String getCustomerId() { return customerId; }
    public String getDisplayName() { return displayName; }
}
