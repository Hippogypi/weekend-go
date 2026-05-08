package com.weekendgo.auth;

import java.util.Optional;

public interface UserAccountRepository {

    UserAccount save(String username, String passwordHash, UserRole role, String nickname);

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findById(long id);

    boolean existsByUsername(String username);
}
