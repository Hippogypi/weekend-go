package com.weekendgo.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weekendgo.interaction.PendingAuditItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class JdbcWorkspaceProfileRepository implements WorkspaceProfileRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public JdbcWorkspaceProfileRepository(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProfileSubmission createSubmission(long placeId, long userId, ProfileSubmissionRequest request) {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                var statement = connection.prepareStatement("""
                        INSERT INTO profile_submissions (
                          place_id, user_id, quiet_score, wifi_score, socket_score, seat_score, cost_score,
                          min_consumption, allow_long_stay, suitable_scenes, remark, audit_status
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, placeId);
                statement.setLong(2, userId);
                statement.setBigDecimal(3, request.quietScore());
                statement.setBigDecimal(4, request.wifiScore());
                statement.setBigDecimal(5, request.socketScore());
                statement.setBigDecimal(6, request.seatScore());
                statement.setBigDecimal(7, request.costScore());
                if (request.minConsumption() == null) {
                    statement.setObject(8, null);
                } else {
                    statement.setInt(8, request.minConsumption());
                }
                statement.setString(9, request.normalizedAllowLongStay().name());
                statement.setString(10, writeScenes(request.normalizedSuitableScenes()));
                statement.setString(11, request.remark());
                return statement;
            }, keyHolder);
            Number generatedId = null;
            if (keyHolder.getKeys() != null) {
                generatedId = (Number) keyHolder.getKeys().get("id");
            }
            if (generatedId == null) {
                generatedId = keyHolder.getKey();
            }
            return findSubmissionById(generatedId.longValue()).orElseThrow(ProfileSubmissionNotFoundException::new);
        } catch (DataAccessException exception) {
            throw new ProfileStorageException("Failed to create profile submission", exception);
        }
    }

    @Override
    public ProfileSubmission audit(long submissionId, long adminId, AuditStatus status, String reason) {
        if (status != AuditStatus.APPROVED && status != AuditStatus.REJECTED) {
            throw new IllegalArgumentException("Only approve and reject audit actions are supported");
        }
        try {
            return transactionTemplate.execute(transactionStatus -> {
                ProfileSubmission current = findSubmissionById(submissionId)
                        .orElseThrow(ProfileSubmissionNotFoundException::new);
                int updated = jdbcTemplate.update("""
                        UPDATE profile_submissions
                        SET audit_status = ?, audited_by = ?, audited_at = CURRENT_TIMESTAMP, audit_reason = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND audit_status = 'PENDING'
                        """, status.name(), adminId, reason, submissionId);
                if (updated == 0) {
                    throw new ProfileSubmissionNotFoundException();
                }
                jdbcTemplate.update("""
                        INSERT INTO audit_logs (target_type, target_id, admin_id, action, reason)
                        VALUES ('PROFILE_SUBMISSION', ?, ?, ?, ?)
                        """, submissionId, adminId, status.name(), reason);
                if (status == AuditStatus.APPROVED) {
                    rebuildProfile(current.placeId());
                }
                return findSubmissionById(submissionId).orElseThrow(ProfileSubmissionNotFoundException::new);
            });
        } catch (ProfileSubmissionNotFoundException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw new ProfileStorageException("Failed to audit profile submission", exception);
        }
    }

    @Override
    public Optional<ProfileSubmission> findSubmissionById(long submissionId) {
        try {
            return jdbcTemplate.query("""
                    SELECT id, place_id, user_id, quiet_score, wifi_score, socket_score, seat_score,
                           cost_score, min_consumption, allow_long_stay, suitable_scenes, remark,
                           audit_status, audited_by, audited_at, audit_reason, created_at
                    FROM profile_submissions
                    WHERE id = ?
                    """, this::mapSubmission, submissionId).stream().findFirst();
        } catch (DataAccessException exception) {
            throw new ProfileStorageException("Failed to load profile submission", exception);
        }
    }

    @Override
    public List<PendingAuditItem> findPendingProfileSubmissions(int page, int size) {
        try {
            return jdbcTemplate.query("""
                    SELECT ps.id, ps.place_id, p.name AS place_name, ps.user_id, u.username, ps.remark AS content, ps.created_at
                    FROM profile_submissions ps
                    JOIN places p ON p.id = ps.place_id
                    JOIN users u ON u.id = ps.user_id
                    WHERE ps.audit_status = 'PENDING'
                    ORDER BY ps.created_at DESC, ps.id DESC
                    LIMIT ? OFFSET ?
                    """, (resultSet, rowNum) -> new PendingAuditItem(
                    resultSet.getLong("id"),
                    resultSet.getLong("place_id"),
                    resultSet.getString("place_name"),
                    resultSet.getLong("user_id"),
                    resultSet.getString("username"),
                    resultSet.getString("content"),
                    nullableInstant(resultSet, "created_at"),
                    "profile"
            ), size, (page - 1) * size);
        } catch (DataAccessException exception) {
            throw new ProfileStorageException("Failed to load pending profile submissions", exception);
        }
    }

    @Override
    public long countPendingProfileSubmissions() {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM profile_submissions WHERE audit_status = 'PENDING'", Long.class);
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            throw new ProfileStorageException("Failed to count pending profile submissions", exception);
        }
    }

    @Override
    public Optional<WorkspaceProfile> findProfileByPlaceId(long placeId) {
        try {
            return jdbcTemplate.query("""
                    SELECT place_id, quiet_score, wifi_score, socket_score, seat_score, cost_score,
                           min_consumption, allow_long_stay, score, trust_level,
                           approved_submission_count, contributor_count, last_contributed_at
                    FROM workspace_profiles
                    WHERE place_id = ?
                    """, this::mapProfile, placeId).stream().findFirst();
        } catch (DataAccessException exception) {
            throw new ProfileStorageException("Failed to load workspace profile", exception);
        }
    }

    @Override
    public List<ProfileSubmission> findSubmissionsByUserId(long userId) {
        try {
            return jdbcTemplate.query("""
                    SELECT id, place_id, user_id, quiet_score, wifi_score, socket_score, seat_score,
                           cost_score, min_consumption, allow_long_stay, suitable_scenes, remark,
                           audit_status, audited_by, audited_at, audit_reason, created_at
                    FROM profile_submissions
                    WHERE user_id = ?
                    ORDER BY created_at DESC, id DESC
                    """, this::mapSubmission, userId);
        } catch (DataAccessException exception) {
            throw new ProfileStorageException("Failed to load profile submissions", exception);
        }
    }

    private void rebuildProfile(long placeId) {
        Aggregation aggregation = aggregate(placeId);
        int updated = jdbcTemplate.update("""
                UPDATE workspace_profiles
                SET quiet_score = ?, wifi_score = ?, socket_score = ?, seat_score = ?, cost_score = ?,
                    min_consumption = ?, allow_long_stay = ?, score = ?, trust_level = ?,
                    approved_submission_count = ?, contributor_count = ?, last_contributed_at = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE place_id = ?
                """,
                aggregation.quietScore(),
                aggregation.wifiScore(),
                aggregation.socketScore(),
                aggregation.seatScore(),
                aggregation.costScore(),
                aggregation.minConsumption(),
                aggregation.allowLongStay().name(),
                aggregation.score(),
                aggregation.trustLevel().name(),
                aggregation.approvedSubmissionCount(),
                aggregation.contributorCount(),
                Timestamp.from(aggregation.lastContributedAt()),
                placeId
        );
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO workspace_profiles (
                      place_id, quiet_score, wifi_score, socket_score, seat_score, cost_score,
                      min_consumption, allow_long_stay, score, trust_level,
                      approved_submission_count, contributor_count, last_contributed_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    placeId,
                    aggregation.quietScore(),
                    aggregation.wifiScore(),
                    aggregation.socketScore(),
                    aggregation.seatScore(),
                    aggregation.costScore(),
                    aggregation.minConsumption(),
                    aggregation.allowLongStay().name(),
                    aggregation.score(),
                    aggregation.trustLevel().name(),
                    aggregation.approvedSubmissionCount(),
                    aggregation.contributorCount(),
                    Timestamp.from(aggregation.lastContributedAt())
            );
        }
    }

    private Aggregation aggregate(long placeId) {
        return jdbcTemplate.queryForObject("""
                SELECT AVG(quiet_score) quiet_score,
                       AVG(wifi_score) wifi_score,
                       AVG(socket_score) socket_score,
                       AVG(seat_score) seat_score,
                       AVG(cost_score) cost_score,
                       MIN(min_consumption) min_consumption,
                       SUM(CASE WHEN allow_long_stay = 'TRUE' THEN 1 ELSE 0 END) true_count,
                       SUM(CASE WHEN allow_long_stay = 'FALSE' THEN 1 ELSE 0 END) false_count,
                       COUNT(*) approved_submission_count,
                       COUNT(DISTINCT user_id) contributor_count,
                       MAX(created_at) last_contributed_at
                FROM profile_submissions
                WHERE place_id = ? AND audit_status = 'APPROVED'
                """, (resultSet, rowNum) -> {
                    BigDecimal quiet = rounded(resultSet.getBigDecimal("quiet_score"), 1);
                    BigDecimal wifi = rounded(resultSet.getBigDecimal("wifi_score"), 1);
                    BigDecimal socket = rounded(resultSet.getBigDecimal("socket_score"), 1);
                    BigDecimal seat = rounded(resultSet.getBigDecimal("seat_score"), 1);
                    BigDecimal cost = rounded(resultSet.getBigDecimal("cost_score"), 1);
                    int approvedCount = resultSet.getInt("approved_submission_count");
                    int trueCount = resultSet.getInt("true_count");
                    int falseCount = resultSet.getInt("false_count");
                    return new Aggregation(
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
                            resultSet.getTimestamp("last_contributed_at").toInstant()
                    );
                }, placeId);
    }

    private ProfileSubmission mapSubmission(ResultSet resultSet, int rowNum) throws SQLException {
        long auditedBy = resultSet.getLong("audited_by");
        return new ProfileSubmission(
                resultSet.getLong("id"),
                resultSet.getLong("place_id"),
                resultSet.getLong("user_id"),
                resultSet.getBigDecimal("quiet_score"),
                resultSet.getBigDecimal("wifi_score"),
                resultSet.getBigDecimal("socket_score"),
                resultSet.getBigDecimal("seat_score"),
                resultSet.getBigDecimal("cost_score"),
                nullableInt(resultSet, "min_consumption"),
                AllowLongStay.valueOf(resultSet.getString("allow_long_stay")),
                readScenes(resultSet.getString("suitable_scenes")),
                resultSet.getString("remark"),
                AuditStatus.valueOf(resultSet.getString("audit_status")),
                resultSet.wasNull() ? null : auditedBy,
                nullableInstant(resultSet, "audited_at"),
                resultSet.getString("audit_reason"),
                nullableInstant(resultSet, "created_at")
        );
    }

    private WorkspaceProfile mapProfile(ResultSet resultSet, int rowNum) throws SQLException {
        return new WorkspaceProfile(
                resultSet.getLong("place_id"),
                resultSet.getBigDecimal("quiet_score"),
                resultSet.getBigDecimal("wifi_score"),
                resultSet.getBigDecimal("socket_score"),
                resultSet.getBigDecimal("seat_score"),
                resultSet.getBigDecimal("cost_score"),
                nullableInt(resultSet, "min_consumption"),
                AllowLongStay.valueOf(resultSet.getString("allow_long_stay")),
                resultSet.getBigDecimal("score"),
                TrustLevel.valueOf(resultSet.getString("trust_level")),
                resultSet.getInt("approved_submission_count"),
                resultSet.getInt("contributor_count"),
                nullableInstant(resultSet, "last_contributed_at")
        );
    }

    private String writeScenes(List<String> scenes) {
        try {
            return objectMapper.writeValueAsString(scenes);
        } catch (JsonProcessingException exception) {
            throw new ProfileStorageException("Failed to serialize suitable scenes", exception);
        }
    }

    private List<String> readScenes(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            if (raw.startsWith("\"")) {
                raw = objectMapper.readValue(raw, String.class);
            }
            return objectMapper.readValue(raw, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new ProfileStorageException("Failed to deserialize suitable scenes", exception);
        }
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

    private record Aggregation(
            BigDecimal quietScore,
            BigDecimal wifiScore,
            BigDecimal socketScore,
            BigDecimal seatScore,
            BigDecimal costScore,
            Integer minConsumption,
            AllowLongStay allowLongStay,
            BigDecimal score,
            TrustLevel trustLevel,
            int approvedSubmissionCount,
            int contributorCount,
            Instant lastContributedAt
    ) {
    }
}
