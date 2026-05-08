package com.weekendgo.auth;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class JdbcUserAccountRepository implements UserAccountRepository {

    private static final String INSERT_SQL = """
            INSERT INTO users (username, password_hash, role, nickname, enabled)
            VALUES (?, ?, ?, ?, 1)
            """;

    private static final String SELECT_COLUMNS = """
            SELECT id, username, password_hash, role, nickname, enabled, created_at
            FROM users
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JdbcUserAccountRepository(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public UserAccount save(String username, String passwordHash, UserRole role, String nickname) {
        try {
            UserAccount account = transactionTemplate.execute(status -> {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement statement = connection.prepareStatement(INSERT_SQL, new String[]{"id"});
                    statement.setString(1, username);
                    statement.setString(2, passwordHash);
                    statement.setString(3, role.name());
                    statement.setString(4, nickname);
                    return statement;
                }, keyHolder);

                Number key = keyHolder.getKey();
                if (key == null) {
                    throw new IllegalStateException("Failed to retrieve generated user id");
                }
                return findById(key.longValue()).orElseThrow();
            });
            return account == null ? findByUsername(username).orElseThrow() : account;
        } catch (DuplicateKeyException exception) {
            throw new DuplicateUsernameException();
        }
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return jdbcTemplate.query(
                        SELECT_COLUMNS + "WHERE LOWER(username) = LOWER(?)",
                        this::mapUserAccount,
                        username
                )
                .stream()
                .findFirst();
    }

    @Override
    public Optional<UserAccount> findById(long id) {
        return jdbcTemplate.query(
                        SELECT_COLUMNS + "WHERE id = ?",
                        this::mapUserAccount,
                        id
                )
                .stream()
                .findFirst();
    }

    @Override
    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?)",
                Integer.class,
                username
        );
        return count != null && count > 0;
    }

    private UserAccount mapUserAccount(ResultSet resultSet, int rowNum) throws SQLException {
        return new UserAccount(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
                UserRole.valueOf(resultSet.getString("role")),
                resultSet.getString("nickname"),
                resultSet.getBoolean("enabled"),
                toInstant(resultSet.getTimestamp("created_at"))
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
