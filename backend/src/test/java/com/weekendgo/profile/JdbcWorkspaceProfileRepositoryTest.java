package com.weekendgo.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class JdbcWorkspaceProfileRepositoryTest {

    private static final String JDBC_URL = "jdbc:h2:mem:profiles;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private JdbcTemplate jdbcTemplate;
    private JdbcWorkspaceProfileRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(JDBC_URL);
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcWorkspaceProfileRepository(
                jdbcTemplate,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)),
                new ObjectMapper()
        );

        jdbcTemplate.execute("DROP TABLE IF EXISTS audit_logs");
        jdbcTemplate.execute("DROP TABLE IF EXISTS workspace_profiles");
        jdbcTemplate.execute("DROP TABLE IF EXISTS profile_submissions");
        jdbcTemplate.execute("DROP TABLE IF EXISTS places");
        jdbcTemplate.execute("DROP TABLE IF EXISTS users");
        createTables();
        seedPlaceAndUsers();
    }

    @Test
    void createSubmissionStoresPendingStatus() {
        ProfileSubmission submission = repository.createSubmission(42, 7, new ProfileSubmissionRequest(
                new BigDecimal("4.5"),
                new BigDecimal("4.0"),
                new BigDecimal("3.5"),
                new BigDecimal("4.0"),
                new BigDecimal("3.0"),
                20,
                AllowLongStay.TRUE,
                List.of("READING", "REMOTE_WORK"),
                "stable wifi"
        ));

        assertThat(submission.auditStatus()).isEqualTo(AuditStatus.PENDING);
        assertThat(submission.placeId()).isEqualTo(42);
        assertThat(submission.userId()).isEqualTo(7);
        assertThat(submission.suitableScenes()).containsExactly("READING", "REMOTE_WORK");
    }

    @Test
    void approveSubmissionRebuildsAggregatedPublicWorkspaceProfile() {
        ProfileSubmission first = repository.createSubmission(42, 7, new ProfileSubmissionRequest(
                new BigDecimal("4.0"),
                new BigDecimal("5.0"),
                new BigDecimal("3.0"),
                new BigDecimal("4.0"),
                new BigDecimal("2.0"),
                30,
                AllowLongStay.TRUE,
                List.of("READING"),
                "first"
        ));
        ProfileSubmission second = repository.createSubmission(42, 8, new ProfileSubmissionRequest(
                new BigDecimal("2.0"),
                new BigDecimal("3.0"),
                new BigDecimal("5.0"),
                new BigDecimal("4.0"),
                null,
                10,
                AllowLongStay.FALSE,
                List.of("REMOTE_WORK"),
                "second"
        ));

        repository.audit(first.id(), 99, AuditStatus.APPROVED, "ok");
        repository.audit(second.id(), 99, AuditStatus.REJECTED, "noisy");

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

        Integer auditLogs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_logs", Integer.class);
        assertThat(auditLogs).isEqualTo(2);
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
                CREATE TABLE profile_submissions (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  place_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  quiet_score DECIMAL(2, 1) NOT NULL,
                  wifi_score DECIMAL(2, 1) NOT NULL,
                  socket_score DECIMAL(2, 1) NOT NULL,
                  seat_score DECIMAL(2, 1) NOT NULL,
                  cost_score DECIMAL(2, 1),
                  min_consumption INT,
                  allow_long_stay VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
                  suitable_scenes JSON,
                  remark VARCHAR(500),
                  audit_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                  audited_by BIGINT,
                  audited_at DATETIME,
                  audit_reason VARCHAR(500),
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE workspace_profiles (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  place_id BIGINT NOT NULL UNIQUE,
                  quiet_score DECIMAL(2, 1),
                  wifi_score DECIMAL(2, 1),
                  socket_score DECIMAL(2, 1),
                  seat_score DECIMAL(2, 1),
                  cost_score DECIMAL(2, 1),
                  min_consumption INT,
                  allow_long_stay VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
                  score DECIMAL(3, 2),
                  trust_level VARCHAR(16) NOT NULL DEFAULT 'LOW',
                  approved_submission_count INT NOT NULL DEFAULT 0,
                  contributor_count INT NOT NULL DEFAULT 0,
                  last_contributed_at DATETIME,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE audit_logs (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  target_type VARCHAR(32) NOT NULL,
                  target_id BIGINT NOT NULL,
                  admin_id BIGINT NOT NULL,
                  action VARCHAR(16) NOT NULL,
                  reason VARCHAR(500),
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
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, role) VALUES (99, 'root', 'x', 'ADMIN')"
        );
    }
}
