package com.weekendgo.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class JdbcUserAccountRepositoryTest {

    private static final String JDBC_URL = "jdbc:h2:mem:users;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private JdbcUserAccountRepository repository;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(JDBC_URL);
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcUserAccountRepository(
                jdbcTemplate,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource))
        );

        jdbcTemplate.execute("DROP TABLE IF EXISTS users");
        jdbcTemplate.execute("""
                    CREATE TABLE users (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      username VARCHAR(64) NOT NULL UNIQUE,
                      password_hash VARCHAR(255) NOT NULL,
                      role VARCHAR(16) NOT NULL DEFAULT 'USER',
                      nickname VARCHAR(64),
                      enabled TINYINT(1) NOT NULL DEFAULT 1,
                      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
    }

    @Test
    void savePersistsUserAndLoadsItByUsernameIgnoringCase() {
        UserAccount saved = repository.save("Alice", "$2a$hash", UserRole.USER, "Alice");

        assertThat(saved.id()).isPositive();
        assertThat(saved.username()).isEqualTo("Alice");
        assertThat(saved.passwordHash()).isEqualTo("$2a$hash");
        assertThat(saved.role()).isEqualTo(UserRole.USER);
        assertThat(saved.nickname()).isEqualTo("Alice");
        assertThat(saved.enabled()).isTrue();
        assertThat(saved.createdAt()).isNotNull();

        assertThat(repository.findByUsername("alice")).contains(saved);
        assertThat(repository.findById(saved.id())).contains(saved);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
    }

    @Test
    void saveMapsDuplicateUsernameToDomainException() {
        repository.save("bob", "$2a$hash", UserRole.USER, "Bob");

        assertThatThrownBy(() -> repository.save("bob", "$2a$other", UserRole.USER, "Bobby"))
                .isInstanceOf(DuplicateUsernameException.class);
    }

    @Test
    void existsByUsernameUsesCaseInsensitiveLookup() {
        repository.save("Carol", "$2a$hash", UserRole.ADMIN, "Carol");

        assertThat(repository.existsByUsername("carol")).isTrue();
        assertThat(repository.existsByUsername("missing")).isFalse();
    }
}
