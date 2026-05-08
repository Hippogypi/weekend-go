package com.weekendgo.place;

import com.weekendgo.amap.dto.AmapPoi;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class JdbcPlaceRepository implements PlaceRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO places (
              amap_poi_id, name, address, longitude, latitude, amap_type, district, source, workspace_status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, 'AMAP_SEARCH', 'CANDIDATE')
            ON DUPLICATE KEY UPDATE
              name = VALUES(name),
              address = VALUES(address),
              longitude = VALUES(longitude),
              latitude = VALUES(latitude),
              amap_type = VALUES(amap_type),
              district = VALUES(district),
              updated_at = CURRENT_TIMESTAMP
            """;

    private static final String SELECT_BY_AMAP_POI_ID_SQL = """
            SELECT id, amap_poi_id, name, address, longitude, latitude, amap_type, amap_type_code,
                   province, city, district, source, workspace_status
            FROM places
            WHERE amap_poi_id = ?
            """;

    private static final String SELECT_BY_ID_SQL = """
            SELECT id, amap_poi_id, name, address, longitude, latitude, amap_type, amap_type_code,
                   province, city, district, source, workspace_status
            FROM places
            WHERE id = ?
            """;

    private final ConnectionFactory connectionFactory;

    public JdbcPlaceRepository(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username:}") String username,
            @Value("${spring.datasource.password:}") String password
    ) {
        this(() -> DriverManager.getConnection(url, username, password));
    }

    JdbcPlaceRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public List<Place> saveAllFromAmap(List<AmapPoi> pois) {
        Map<String, AmapPoi> poisById = deduplicate(pois);
        if (poisById.isEmpty()) {
            return List.of();
        }

        try (Connection connection = connectionFactory.open()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                upsertPois(connection, poisById.values());
                List<Place> places = findByAmapPoiIds(connection, poisById.keySet().stream().toList());
                connection.commit();
                return places;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw new PlaceStorageException("Failed to persist Amap places", exception);
        }
    }

    @Override
    public Optional<Place> findById(long id) {
        try (Connection connection = connectionFactory.open();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapPlace(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new PlaceStorageException("Failed to load place", exception);
        }
    }

    private void upsertPois(Connection connection, Iterable<AmapPoi> pois) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
            for (AmapPoi poi : pois) {
                Coordinates coordinates = Coordinates.parse(poi.location());
                statement.setString(1, poi.id());
                statement.setString(2, poi.name());
                statement.setString(3, poi.address());
                statement.setBigDecimal(4, coordinates.longitude());
                statement.setBigDecimal(5, coordinates.latitude());
                statement.setString(6, poi.type());
                statement.setString(7, poi.district());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private List<Place> findByAmapPoiIds(Connection connection, List<String> amapPoiIds) throws SQLException {
        List<Place> places = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_AMAP_POI_ID_SQL)) {
            for (String amapPoiId : amapPoiIds) {
                statement.setString(1, amapPoiId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        places.add(mapPlace(resultSet));
                    }
                }
            }
        }
        return places;
    }

    private Map<String, AmapPoi> deduplicate(List<AmapPoi> pois) {
        Map<String, AmapPoi> poisById = new LinkedHashMap<>();
        for (AmapPoi poi : pois) {
            if (StringUtils.hasText(poi.id()) && StringUtils.hasText(poi.location())) {
                poisById.putIfAbsent(poi.id(), poi);
            }
        }
        return poisById;
    }

    private Place mapPlace(ResultSet resultSet) throws SQLException {
        return new Place(
                resultSet.getLong("id"),
                resultSet.getString("amap_poi_id"),
                resultSet.getString("name"),
                resultSet.getString("address"),
                resultSet.getBigDecimal("longitude"),
                resultSet.getBigDecimal("latitude"),
                resultSet.getString("amap_type"),
                resultSet.getString("amap_type_code"),
                resultSet.getString("province"),
                resultSet.getString("city"),
                resultSet.getString("district"),
                PlaceSource.valueOf(resultSet.getString("source")),
                WorkspaceStatus.valueOf(resultSet.getString("workspace_status"))
        );
    }

    @FunctionalInterface
    interface ConnectionFactory {
        Connection open() throws SQLException;
    }

    private record Coordinates(BigDecimal longitude, BigDecimal latitude) {

        static Coordinates parse(String location) {
            String[] parts = location.split(",", 2);
            if (parts.length != 2) {
                throw new PlaceStorageException("Invalid Amap location: " + location);
            }
            return new Coordinates(new BigDecimal(parts[0]), new BigDecimal(parts[1]));
        }
    }
}
