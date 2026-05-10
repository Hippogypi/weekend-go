package com.weekendgo.interaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.weekendgo.place.Place;
import com.weekendgo.place.PlaceSource;
import com.weekendgo.place.WorkspaceStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class JdbcInteractionRepositoryTest {

    private static final String JDBC_URL =
            "jdbc:h2:mem:interactions;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private JdbcInteractionRepository repository;
    private JdbcTemplate jdbcTemplate;
    private Place place;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(JDBC_URL);
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcInteractionRepository(
                jdbcTemplate,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource))
        );

        jdbcTemplate.execute("DROP TABLE IF EXISTS audit_logs");
        jdbcTemplate.execute("DROP TABLE IF EXISTS favorites");
        jdbcTemplate.execute("DROP TABLE IF EXISTS place_images");
        jdbcTemplate.execute("DROP TABLE IF EXISTS reviews");
        jdbcTemplate.execute("DROP TABLE IF EXISTS users");
        jdbcTemplate.execute("DROP TABLE IF EXISTS places");
        createTables();
        jdbcTemplate.update("""
                INSERT INTO users (id, username, password_hash, role, nickname, enabled)
                VALUES (1, 'user', 'hash', 'USER', 'User', 1),
                       (2, 'admin', 'hash', 'ADMIN', 'Admin', 1)
                """);
        jdbcTemplate.update("""
                INSERT INTO places (
                  id, amap_poi_id, name, address, longitude, latitude, source, workspace_status
                ) VALUES (42, 'B0LIBRARY', 'City Library', 'College Road', 116.300000, 39.900000,
                          'AMAP_SEARCH', 'CANDIDATE')
                """);
        place = new Place(
                42,
                "B0LIBRARY",
                "City Library",
                "College Road",
                new BigDecimal("116.300000"),
                new BigDecimal("39.900000"),
                null,
                null,
                null,
                null,
                null,
                PlaceSource.AMAP_SEARCH,
                WorkspaceStatus.CANDIDATE
        );
    }

    @Test
    void reviewsImagesAndFavoritesUseSchemaTables() {
        ReviewResponse review = repository.createReview(42, 1, new ReviewRequest(
                new BigDecimal("4.5"),
                new BigDecimal("4.0"),
                new BigDecimal("5.0"),
                new BigDecimal("4.0"),
                new BigDecimal("3.5"),
                "quiet tables",
                null,
                null
        ));
        ImageResponse image = repository.createImage(42, 1, new ImageRequest(
                "https://example.com/library.jpg",
                "reading area"
        ));

        assertThat(review.auditStatus()).isEqualTo(AuditStatus.PENDING);
        assertThat(image.auditStatus()).isEqualTo(AuditStatus.PENDING);
        assertThat(repository.findApprovedReviews(42)).isEmpty();
        assertThat(repository.findApprovedImages(42)).isEmpty();

        assertThat(repository.auditReview(review.id(), 2, AuditStatus.APPROVED, "ok"))
                .get()
                .extracting(ReviewResponse::auditStatus)
                .isEqualTo(AuditStatus.APPROVED);
        assertThat(repository.auditImage(image.id(), 2, AuditStatus.APPROVED, "ok"))
                .get()
                .extracting(ImageResponse::auditStatus)
                .isEqualTo(AuditStatus.APPROVED);

        assertThat(repository.findApprovedReviews(42)).hasSize(1);
        assertThat(repository.findApprovedImages(42)).hasSize(1);
        assertThat(auditLogCount()).isEqualTo(2);

        repository.favorite(1, place);
        repository.favorite(1, place);
        assertThat(repository.isFavorited(1, 42)).isTrue();
        assertThat(repository.findFavorites(1))
                .extracting(FavoritePlaceResponse::placeName)
                .containsExactly("City Library");
        repository.unfavorite(1, 42);
        assertThat(repository.isFavorited(1, 42)).isFalse();
    }

    @Test
    void saveImageWithReviewIdAndFindImagesByReviewId() {
        ReviewResponse review = repository.createReview(42, 1, new ReviewRequest(
                new BigDecimal("4.5"),
                new BigDecimal("4.0"),
                new BigDecimal("5.0"),
                new BigDecimal("4.0"),
                new BigDecimal("3.5"),
                "quiet tables",
                null,
                null
        ));

        ImageResponse image = repository.saveImageWithReviewId(
                42, 1, review.id(),
                "https://example.com/review.jpg", "review photo"
        );

        assertThat(image.placeId()).isEqualTo(42);
        assertThat(image.auditStatus()).isEqualTo(AuditStatus.PENDING);

        List<ImageResponse> found = repository.findImagesByReviewId(review.id());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).imageUrl()).isEqualTo("https://example.com/review.jpg");

        repository.auditImage(image.id(), 2, AuditStatus.APPROVED, "ok");
        assertThat(repository.findImagesByReviewId(review.id())).hasSize(1);
    }

    @Test
    void findApprovedReviewsIncludesImages() {
        ReviewResponse review = repository.createReview(42, 1, new ReviewRequest(
                new BigDecimal("4.5"),
                new BigDecimal("4.0"),
                new BigDecimal("5.0"),
                new BigDecimal("4.0"),
                new BigDecimal("3.5"),
                "quiet tables",
                null,
                null
        ));
        ImageResponse imageA = repository.saveImageWithReviewId(42, 1, review.id(), "https://example.com/a.jpg", "a");
        ImageResponse imageB = repository.saveImageWithReviewId(42, 1, review.id(), "https://example.com/b.jpg", "b");

        repository.auditReview(review.id(), 2, AuditStatus.APPROVED, "ok");
        repository.auditImage(imageA.id(), 2, AuditStatus.APPROVED, "ok");
        repository.auditImage(imageB.id(), 2, AuditStatus.APPROVED, "ok");
        List<ReviewResponse> approved = repository.findApprovedReviews(42);
        assertThat(approved).hasSize(1);
        assertThat(approved.get(0).images()).hasSize(2);
    }

    private Integer auditLogCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_logs", Integer.class);
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE users (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  username VARCHAR(64) NOT NULL UNIQUE,
                  password_hash VARCHAR(255) NOT NULL,
                  role VARCHAR(16) NOT NULL DEFAULT 'USER',
                  nickname VARCHAR(64),
                  enabled TINYINT NOT NULL DEFAULT 1,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE places (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  amap_poi_id VARCHAR(64) NOT NULL UNIQUE,
                  name VARCHAR(128) NOT NULL,
                  address VARCHAR(255),
                  longitude DECIMAL(10, 6) NOT NULL,
                  latitude DECIMAL(10, 6) NOT NULL,
                  source VARCHAR(32) NOT NULL DEFAULT 'AMAP_SEARCH',
                  workspace_status VARCHAR(32) NOT NULL DEFAULT 'CANDIDATE'
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
                  audit_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                  audited_by BIGINT,
                  audited_at DATETIME,
                  audit_reason VARCHAR(500),
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE place_images (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  place_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  review_id BIGINT,
                  image_url VARCHAR(512) NOT NULL,
                  description VARCHAR(500),
                  audit_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                  audited_by BIGINT,
                  audited_at DATETIME,
                  audit_reason VARCHAR(500),
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE favorites (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id BIGINT NOT NULL,
                  place_id BIGINT NOT NULL,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE (user_id, place_id)
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
}
