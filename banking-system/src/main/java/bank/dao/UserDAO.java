package bank.dao;

import bank.model.LoginRecord;

import java.util.Optional;

public interface UserDAO {
    void save(LoginRecord record);
    Optional<LoginRecord> findByUsername(String username);
}
