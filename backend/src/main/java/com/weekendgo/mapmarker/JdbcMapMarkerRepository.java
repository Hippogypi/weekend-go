package com.weekendgo.mapmarker;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.datasource.url")
public class JdbcMapMarkerRepository implements MapMarkerRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMapMarkerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MapMarkerResponse> findNearbyMarkers(BigDecimal longitude, BigDecimal latitude, double radiusMeters, Long userId) {
        double deltaLat = radiusMeters / 111320.0;
        double deltaLon = radiusMeters / (111320.0 * Math.cos(Math.toRadians(latitude.doubleValue())));
        double minLat = latitude.doubleValue() - deltaLat;
        double maxLat = latitude.doubleValue() + deltaLat;
        double minLon = longitude.doubleValue() - deltaLon;
        double maxLon = longitude.doubleValue() + deltaLon;

        Map<Long, MapMarkerResponse> resultMap = new HashMap<>();

        String markedSql = """
            SELECT DISTINCT p.id, p.name, p.longitude, p.latitude, p.address
            FROM places p
            WHERE p.longitude BETWEEN ? AND ?
              AND p.latitude BETWEEN ? AND ?
              AND (
                EXISTS (SELECT 1 FROM workspace_profiles wp WHERE wp.place_id = p.id AND wp.approved_submission_count > 0)
                OR EXISTS (SELECT 1 FROM checkins c WHERE c.place_id = p.id)
                OR EXISTS (SELECT 1 FROM reviews r WHERE r.place_id = p.id AND r.audit_status = 'APPROVED')
              )
            """;

        List<MapMarkerResponse> marked = jdbcTemplate.query(markedSql, (rs, rowNum) -> new MapMarkerResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getBigDecimal("longitude"),
                rs.getBigDecimal("latitude"),
                rs.getString("address"),
                true,
                false
        ), minLon, maxLon, minLat, maxLat);

        for (MapMarkerResponse m : marked) {
            resultMap.put(m.id(), m);
        }

        if (userId != null) {
            String favoriteSql = """
                SELECT p.id, p.name, p.longitude, p.latitude, p.address
                FROM places p
                JOIN favorites f ON f.place_id = p.id
                WHERE f.user_id = ?
                  AND p.longitude BETWEEN ? AND ?
                  AND p.latitude BETWEEN ? AND ?
                """;

            jdbcTemplate.query(favoriteSql, (rs, rowNum) -> {
                long id = rs.getLong("id");
                if (resultMap.containsKey(id)) {
                    MapMarkerResponse existing = resultMap.get(id);
                    resultMap.put(id, new MapMarkerResponse(
                            existing.id(),
                            existing.name(),
                            existing.longitude(),
                            existing.latitude(),
                            existing.address(),
                            true,
                            true
                    ));
                } else {
                    resultMap.put(id, new MapMarkerResponse(
                            id,
                            rs.getString("name"),
                            rs.getBigDecimal("longitude"),
                            rs.getBigDecimal("latitude"),
                            rs.getString("address"),
                            false,
                            true
                    ));
                }
                return null;
            }, userId, minLon, maxLon, minLat, maxLat);
        }

        double centerLon = longitude.doubleValue();
        double centerLat = latitude.doubleValue();

        return resultMap.values().stream()
                .filter(m -> {
                    double d = haversine(centerLon, centerLat, m.longitude().doubleValue(), m.latitude().doubleValue());
                    return d <= radiusMeters;
                })
                .toList();
    }

    private double haversine(double lon1, double lat1, double lon2, double lat2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
