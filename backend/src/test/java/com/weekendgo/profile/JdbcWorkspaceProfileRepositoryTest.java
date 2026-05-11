package com.weekendgo.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcWorkspaceProfileRepositoryTest {

    private static final String JDBC_URL = "jdbc:h2:mem:profiles;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private JdbcTemplate jdbcTemplate;
    private JdbcWorkspaceProfileRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(JDBC_URL);
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcWorkspaceProfileRepository(jdbcTemplate);

        jdbcTemplate.execute("DROP TABLE IF EXISTS reviews");
        jdbcTemplate.execute("DROP TABLE IF EXISTS places");
        jdbcTemplate.execute("DROP TABLE IF EXISTS users");
        createTables();
        seedPlaceAndUsers();
    }

    @Test
    void aggregatesPublicWorkspaceProfileFromApprovedReviews() {
        insertReview(42, 7, new BigDecimal("4.0"), new BigDecimal("5.0"), new BigDecimal("3.0"),
                new BigDecimal("4.0"), new BigDecimal("2.0"), 30, "TRUE", "APPROVED");
        insertReview(42, 8, new BigDecimal("2.0"), new BigDecimal("3.0"), new BigDecimal("5.0"),
                new BigDecimal("4.0"), new BigDecimal("4.0"), 10, "FALSE", "REJECTED");

        WorkspaceProfile profile = repository.findProfileByPlaceId(42).orElseThrow();

        assertThat(profile.quietScore()).isEqualByComparingTo("4.0");
        assertThat(profile.wifiScore()).isEqualByComparingTo("5.0");
        assertThat(profile.socketScore()).isEqualByComparingTo("3.0");
        assertThat(profile.costScore()).isEqualByComparingTo("2.0");
        assertThat(profile.minConsumption()).isEqualTo(30);
        assertThat(profile.allowLongStay()).isEqualTo(AllowLongStay.TRUE);
        assertThat(profile.score()).isEqualByComparingTo("3.60");
        assertThat(profile.approvedSubmissionCount()).isEqualTo(1);
        assertThat(profile.contributorCount()).isEqualTo(1);
        assertThat(profile.trustLevel()).isEqualTo(TrustLevel.LOW);
    }

    @Test
    void returnsEmptyWhenNoApprovedReviews() {
        assertThat(repository.findProfileByPlaceId(42)).isEmpty();
    }

    private void insertReview(long placeId, long userId, BigDecimal quietScore, BigDecimal wifiScore,
                              BigDecimal socketScore, BigDecimal seatScore, BigDecimal costScore,
                              Integer minConsumption, String allowLongStay, String auditStatus) {
        jdbcTemplate.update("""
                INSERT INTO reviews (
                  place_id, user_id, quiet_score, wifi_score, socket_score, comfort_score, cost_score,
                  content, seat_score, min_consumption, allow_long_stay, audit_status, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'content', ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                placeId, userId, quietScore, wifiScore, socketScore,
                new BigDecimal("4.0"), costScore, seatScore, minConsumption, allowLongStay, auditStatus);
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE users (
                  id BIGINT PRIMARY KEY,
                  username VARCHAR(64) NOT NULL,
                  password_hash VARCHAR(255) NOT NULL,
                  role VARCHAR(16) NOT NULL,
                  enabled BOOLEAN NOT NULL DEFAULT TRUE,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE places (
                  id BIGINT PRIMARY KEY,
                  amap_poi_id VARCHAR(64) NOT NULL,
                  name VARCHAR(128) NOT NULL,
                  longitude DECIMAL(10, 6) NOT NULL,
                  latitude DECIMAL(10, 6) NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE reviews (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  place_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  quiet_score DECIMAL(2, 1) NOT NULL,
                  wifi_score DECIMAL(2, 1) NOT NULL,
                  socket_score DECIMAL(2, 1) NOT NULL,
                  comfort_score DECIMAL(2, 1) NOT NULL,
                  cost_score DECIMAL(2, 1) NOT NULL,
                  content VARCHAR(1000) NOT NULL,
                  seat_score DECIMAL(2, 1),
                  min_consumption INT,
                  allow_long_stay VARCHAR(16) DEFAULT 'UNKNOWN',
                  suitable_scenes JSON,
                  audit_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                  audited_by BIGINT,
                  audited_at DATETIME,
                  audit_reason VARCHAR(500),
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    private void seedPlaceAndUsers() {
        jdbcTemplate.update(
                "INSERT INTO places (id, amap_poi_id, name, longitude, latitude) VALUES (42, 'B0LIBRARY', 'Library', 116.3, 39.9)"
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, role) VALUES (7, 'alice', 'x', 'USER')"
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, role) VALUES (8, 'bob', 'x', 'USER')"
        );
    }
}
