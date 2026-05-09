package com.weekendgo.checkin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class JdbcCheckinRepositoryTest {

    private static final String JDBC_URL = "jdbc:h2:mem:checkins;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private JdbcTemplate jdbcTemplate;
    private JdbcCheckinRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(JDBC_URL);
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcCheckinRepository(
                jdbcTemplate,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource))
        );

        jdbcTemplate.execute("DROP TABLE IF EXISTS checkins");
        jdbcTemplate.execute("""
                    CREATE TABLE checkins (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      place_id BIGINT NOT NULL,
                      user_id BIGINT NOT NULL,
                      crowd_level VARCHAR(32) NOT NULL,
                      noise_level VARCHAR(32) NOT NULL,
                      has_seat TINYINT NOT NULL,
                      remark VARCHAR(500),
                      created_at TIMESTAMP NOT NULL
                    )
                    """);
    }

    @Test
    void savePersistsCheckinFeedback() {
        Instant createdAt = Instant.parse("2026-05-09T06:30:00Z");

        SavedCheckin saved = repository.save(new NewCheckin(
                42,
                7,
                CrowdLevel.NORMAL,
                NoiseLevel.RELATIVELY_QUIET,
                true,
                "two seats near windows",
                createdAt
        ));

        assertThat(saved.id()).isPositive();
        assertThat(saved.placeId()).isEqualTo(42);
        assertThat(saved.userId()).isEqualTo(7);
        assertThat(saved.crowdLevel()).isEqualTo(CrowdLevel.NORMAL);
        assertThat(saved.noiseLevel()).isEqualTo(NoiseLevel.RELATIVELY_QUIET);
        assertThat(saved.hasSeat()).isTrue();
        assertThat(saved.remark()).isEqualTo("two seats near windows");
        assertThat(saved.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void findRecentByPlaceIdOnlyReturnsRowsAtOrAfterCutoff() {
        Instant now = Instant.parse("2026-05-09T06:30:00Z");
        repository.save(new NewCheckin(42, 1, CrowdLevel.FREE, NoiseLevel.QUIET, true, null, now.minusSeconds(3 * 60 * 60)));
        repository.save(new NewCheckin(42, 2, CrowdLevel.CROWDED, NoiseLevel.NOISY, false, null, now.minusSeconds(2 * 60 * 60)));
        repository.save(new NewCheckin(42, 3, CrowdLevel.NORMAL, NoiseLevel.NORMAL, true, null, now.minusSeconds(30 * 60)));
        repository.save(new NewCheckin(99, 4, CrowdLevel.FULL, NoiseLevel.VERY_NOISY, false, null, now.minusSeconds(10 * 60)));

        List<SavedCheckin> recent = repository.findRecentByPlaceId(42, now.minusSeconds(2 * 60 * 60));

        assertThat(recent)
                .extracting(SavedCheckin::userId)
                .containsExactly(3L, 2L);
    }
}
