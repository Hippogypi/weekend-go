package com.weekendgo.mapmarker;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcMapMarkerRepositoryTest {

    private static final String JDBC_URL = "jdbc:h2:mem:mapmarkers;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private JdbcTemplate jdbcTemplate;
    private JdbcMapMarkerRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(JDBC_URL);
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcMapMarkerRepository(jdbcTemplate);

        jdbcTemplate.execute("DROP TABLE IF EXISTS favorites");
        jdbcTemplate.execute("DROP TABLE IF EXISTS reviews");
        jdbcTemplate.execute("DROP TABLE IF EXISTS checkins");
        jdbcTemplate.execute("DROP TABLE IF EXISTS workspace_profiles");
        jdbcTemplate.execute("DROP TABLE IF EXISTS places");
        jdbcTemplate.execute("DROP TABLE IF EXISTS users");

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
                  address VARCHAR(255),
                  longitude DECIMAL(10, 6) NOT NULL,
                  latitude DECIMAL(10, 6) NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE workspace_profiles (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  place_id BIGINT NOT NULL UNIQUE,
                  approved_submission_count INT NOT NULL DEFAULT 0
                )
                """);

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

        seedData();
    }

    private void seedData() {
        jdbcTemplate.update("INSERT INTO users (id, username, password_hash, role) VALUES (1, 'alice', 'x', 'USER')");
        jdbcTemplate.update("INSERT INTO users (id, username, password_hash, role) VALUES (2, 'bob', 'x', 'USER')");

        jdbcTemplate.update("INSERT INTO places (id, amap_poi_id, name, address, longitude, latitude) VALUES (1, 'B01', 'Marked Place', 'Road 1', 116.400000, 39.900000)");
        jdbcTemplate.update("INSERT INTO places (id, amap_poi_id, name, address, longitude, latitude) VALUES (2, 'B02', 'Favorite Place', 'Road 2', 116.401000, 39.901000)");
        jdbcTemplate.update("INSERT INTO places (id, amap_poi_id, name, address, longitude, latitude) VALUES (3, 'B03', 'Both Place', 'Road 3', 116.402000, 39.902000)");
        jdbcTemplate.update("INSERT INTO places (id, amap_poi_id, name, address, longitude, latitude) VALUES (4, 'B04', 'Plain Place', 'Road 4', 116.403000, 39.903000)");
        jdbcTemplate.update("INSERT INTO places (id, amap_poi_id, name, address, longitude, latitude) VALUES (5, 'B05', 'Far Place', 'Road 5', 116.500000, 39.950000)");

        jdbcTemplate.update("INSERT INTO workspace_profiles (place_id, approved_submission_count) VALUES (1, 1)");

        jdbcTemplate.update("INSERT INTO checkins (place_id, user_id, crowd_level, noise_level, has_seat, remark, created_at) VALUES (2, 1, 'NORMAL', 'QUIET', 1, '', CURRENT_TIMESTAMP)");

        jdbcTemplate.update("INSERT INTO reviews (place_id, user_id, quiet_score, wifi_score, socket_score, comfort_score, cost_score, content, audit_status) VALUES (3, 1, 4, 4, 4, 4, 4, 'good', 'APPROVED')");

        jdbcTemplate.update("INSERT INTO favorites (user_id, place_id) VALUES (1, 2)");
        jdbcTemplate.update("INSERT INTO favorites (user_id, place_id) VALUES (1, 3)");
    }

    @Test
    void returnsMarkedPlacesForAnonymousUser() {
        List<MapMarkerResponse> markers = repository.findNearbyMarkers(
                new BigDecimal("116.400000"), new BigDecimal("39.900000"), 5000, null);

        assertThat(markers).hasSize(3);
        assertThat(markers.stream().filter(m -> m.id() == 1L)).allMatch(m -> m.marked() && !m.favorited());
        assertThat(markers.stream().filter(m -> m.id() == 2L)).allMatch(m -> m.marked() && !m.favorited());
        assertThat(markers.stream().filter(m -> m.id() == 3L)).allMatch(m -> m.marked() && !m.favorited());
    }

    @Test
    void returnsFavoritesForLoggedInUser() {
        List<MapMarkerResponse> markers = repository.findNearbyMarkers(
                new BigDecimal("116.400000"), new BigDecimal("39.900000"), 5000, 1L);

        assertThat(markers).hasSize(3);

        MapMarkerResponse place1 = markers.stream().filter(m -> m.id() == 1L).findFirst().orElseThrow();
        assertThat(place1.marked()).isTrue();
        assertThat(place1.favorited()).isFalse();

        MapMarkerResponse place2 = markers.stream().filter(m -> m.id() == 2L).findFirst().orElseThrow();
        assertThat(place2.marked()).isTrue();
        assertThat(place2.favorited()).isTrue();

        MapMarkerResponse place3 = markers.stream().filter(m -> m.id() == 3L).findFirst().orElseThrow();
        assertThat(place3.marked()).isTrue();
        assertThat(place3.favorited()).isTrue();
    }

    @Test
    void excludesOtherUsersFavorites() {
        List<MapMarkerResponse> markers = repository.findNearbyMarkers(
                new BigDecimal("116.400000"), new BigDecimal("39.900000"), 5000, 2L);

        assertThat(markers).hasSize(3);
        assertThat(markers.stream().filter(m -> m.id() == 2L).findFirst().orElseThrow().favorited()).isFalse();
    }

    @Test
    void filtersByDistanceUsingHaversine() {
        List<MapMarkerResponse> markers = repository.findNearbyMarkers(
                new BigDecimal("116.400000"), new BigDecimal("39.900000"), 1000, null);

        assertThat(markers).hasSize(3);
        assertThat(markers.stream().map(MapMarkerResponse::id)).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void farPlaceIsExcludedByHaversine() {
        List<MapMarkerResponse> markers = repository.findNearbyMarkers(
                new BigDecimal("116.400000"), new BigDecimal("39.900000"), 5000, null);

        assertThat(markers.stream().map(MapMarkerResponse::id)).doesNotContain(5L);
    }
}
