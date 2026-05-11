package com.weekendgo.profile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class JdbcWorkspaceProfileRepository implements WorkspaceProfileRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcWorkspaceProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<WorkspaceProfile> findProfileByPlaceId(long placeId) {
        try {
            return jdbcTemplate.query("""
                    SELECT ? AS place_id,
                           AVG(quiet_score) AS quiet_score,
                           AVG(wifi_score) AS wifi_score,
                           AVG(socket_score) AS socket_score,
                           AVG(seat_score) AS seat_score,
                           AVG(cost_score) AS cost_score,
                           MIN(min_consumption) AS min_consumption,
                           SUM(CASE WHEN allow_long_stay = 'TRUE' THEN 1 ELSE 0 END) AS true_count,
                           SUM(CASE WHEN allow_long_stay = 'FALSE' THEN 1 ELSE 0 END) AS false_count,
                           COUNT(*) AS approved_submission_count,
                           COUNT(DISTINCT user_id) AS contributor_count,
                           MAX(created_at) AS last_contributed_at
                    FROM reviews
                    WHERE place_id = ? AND audit_status = 'APPROVED'
                    GROUP BY place_id
                    """, this::mapProfile, placeId, placeId).stream().findFirst();
        } catch (DataAccessException exception) {
            throw new ProfileStorageException("Failed to load workspace profile", exception);
        }
    }

    private WorkspaceProfile mapProfile(ResultSet resultSet, int rowNum) throws SQLException {
        BigDecimal quiet = rounded(resultSet.getBigDecimal("quiet_score"), 1);
        BigDecimal wifi = rounded(resultSet.getBigDecimal("wifi_score"), 1);
        BigDecimal socket = rounded(resultSet.getBigDecimal("socket_score"), 1);
        BigDecimal seat = rounded(resultSet.getBigDecimal("seat_score"), 1);
        BigDecimal cost = rounded(resultSet.getBigDecimal("cost_score"), 1);
        int approvedCount = resultSet.getInt("approved_submission_count");
        int trueCount = resultSet.getInt("true_count");
        int falseCount = resultSet.getInt("false_count");
        return new WorkspaceProfile(
                resultSet.getLong("place_id"),
                quiet,
                wifi,
                socket,
                seat,
                cost,
                nullableInt(resultSet, "min_consumption"),
                aggregateAllowLongStay(trueCount, falseCount),
                profileScore(quiet, wifi, socket, seat, cost),
                trustLevel(approvedCount),
                approvedCount,
                resultSet.getInt("contributor_count"),
                nullableInstant(resultSet, "last_contributed_at")
        );
    }

    private BigDecimal rounded(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private BigDecimal profileScore(BigDecimal... values) {
        List<BigDecimal> present = new ArrayList<>();
        for (BigDecimal value : values) {
            if (value != null) {
                present.add(value);
            }
        }
        if (present.isEmpty()) {
            return null;
        }
        BigDecimal sum = present.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(present.size()), 2, RoundingMode.HALF_UP);
    }

    private AllowLongStay aggregateAllowLongStay(int trueCount, int falseCount) {
        if (trueCount == 0 && falseCount == 0) {
            return AllowLongStay.UNKNOWN;
        }
        return trueCount >= falseCount ? AllowLongStay.TRUE : AllowLongStay.FALSE;
    }

    private TrustLevel trustLevel(int approvedSubmissionCount) {
        if (approvedSubmissionCount >= 10) {
            return TrustLevel.HIGH;
        }
        if (approvedSubmissionCount >= 3) {
            return TrustLevel.MEDIUM;
        }
        return TrustLevel.LOW;
    }

    private Integer nullableInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
