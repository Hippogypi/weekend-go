package com.weekendgo.place;

import static org.assertj.core.api.Assertions.assertThat;

import com.weekendgo.amap.dto.AmapPoi;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class JdbcPlaceRepositoryTest {

    private static final String JDBC_URL = "jdbc:h2:mem:places;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private JdbcPlaceRepository repository;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(JDBC_URL);
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcPlaceRepository(
                jdbcTemplate,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource))
        );

        jdbcTemplate.execute("DROP TABLE IF EXISTS places");
        jdbcTemplate.execute("""
                    CREATE TABLE places (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      amap_poi_id VARCHAR(64) NOT NULL UNIQUE,
                      name VARCHAR(128) NOT NULL,
                      address VARCHAR(255),
                      longitude DECIMAL(10, 6) NOT NULL,
                      latitude DECIMAL(10, 6) NOT NULL,
                      amap_type VARCHAR(255),
                      amap_type_code VARCHAR(32),
                      province VARCHAR(64),
                      city VARCHAR(64),
                      district VARCHAR(64),
                      source VARCHAR(32) NOT NULL DEFAULT 'AMAP_SEARCH',
                      workspace_status VARCHAR(32) NOT NULL DEFAULT 'CANDIDATE',
                      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
    }

    @Test
    void saveAllFromAmapDeduplicatesByAmapPoiIdAndKeepsDatabaseRowUnique() throws Exception {
        List<Place> places = repository.saveAllFromAmap(List.of(
                new AmapPoi("B0DUP", "First Name", "Cafe", "Road 1", "116.100000,39.100000", "Haidian"),
                new AmapPoi("B0DUP", "Duplicate Name", "Cafe", "Road 2", "116.200000,39.200000", "Haidian")
        ));

        assertThat(places).hasSize(1);
        assertThat(places.get(0).amapPoiId()).isEqualTo("B0DUP");
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void saveAllFromAmapUpdatesExistingPlaceAndFindsById() throws Exception {
        Place firstSave = repository.saveAllFromAmap(List.of(
                new AmapPoi("B0UP", "Old Name", "Cafe", "Old Road", "116.100000,39.100000", "Haidian")
        )).get(0);

        Place secondSave = repository.saveAllFromAmap(List.of(
                new AmapPoi("B0UP", "New Name", "Library", "New Road", "116.300000,39.300000", "Chaoyang")
        )).get(0);

        assertThat(secondSave.id()).isEqualTo(firstSave.id());
        assertThat(secondSave.name()).isEqualTo("New Name");
        assertThat(secondSave.address()).isEqualTo("New Road");
        assertThat(secondSave.amapType()).isEqualTo("Library");
        assertThat(secondSave.district()).isEqualTo("Chaoyang");
        assertThat(repository.findById(firstSave.id())).contains(secondSave);
        assertThat(countRows()).isEqualTo(1);
    }

    private int countRows() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM places", Integer.class);
    }
}
