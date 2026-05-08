package com.weekendgo.auth;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final AtomicLong ids = new AtomicLong(1);
    private final Map<Long, UserAccount> usersById = new ConcurrentHashMap<>();
    private final Map<String, Long> idsByUsername = new ConcurrentHashMap<>();

    @Override
    public UserAccount save(String username, String passwordHash, UserRole role, String nickname) {
        String normalizedUsername = normalize(username);
        if (idsByUsername.containsKey(normalizedUsername)) {
            throw new DuplicateUsernameException();
        }

        long id = ids.getAndIncrement();
        UserAccount account = new UserAccount(
                id,
                username,
                passwordHash,
                role,
                nickname,
                true,
                Instant.now()
        );
        idsByUsername.put(normalizedUsername, id);
        usersById.put(id, account);
        return account;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        Long id = idsByUsername.get(normalize(username));
        return id == null ? Optional.empty() : findById(id);
    }

    @Override
    public Optional<UserAccount> findById(long id) {
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public boolean existsByUsername(String username) {
        return idsByUsername.containsKey(normalize(username));
    }

    private String normalize(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
