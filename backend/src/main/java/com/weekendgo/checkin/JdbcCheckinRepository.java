package com.weekendgo.checkin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class JdbcCheckinRepository implements CheckinRepository {

    private static final String INSERT_SQL = """
            INSERT INTO checkins (place_id, user_id, crowd_level, noise_level, has_seat, remark, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_COLUMNS = """
            SELECT id, place_id, user_id, crowd_level, noise_level, has_seat, remark, created_at
            FROM checkins
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JdbcCheckinRepository(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public SavedCheckin save(NewCheckin checkin) {
        try {
            SavedCheckin saved = transactionTemplate.execute(status -> {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement statement = connection.prepareStatement(INSERT_SQL, new String[]{"id"});
                    statement.setLong(1, checkin.placeId());
                    statement.setLong(2, checkin.userId());
                    statement.setString(3, checkin.crowdLevel().name());
                    statement.setString(4, checkin.noiseLevel().name());
                    statement.setBoolean(5, checkin.hasSeat());
                    statement.setString(6, checkin.remark());
                    statement.setTimestamp(7, Timestamp.from(checkin.createdAt()));
                    return statement;
                }, keyHolder);

                Number key = keyHolder.getKey();
                if (key == null) {
                    throw new CheckinStorageException("Failed to retrieve generated checkin id");
                }
                return findById(key.longValue());
            });
            if (saved == null) {
                throw new CheckinStorageException("Failed to persist checkin");
            }
            return saved;
        } catch (DataAccessException exception) {
            throw new CheckinStorageException("Failed to persist checkin", exception);
        }
    }

    @Override
    public List<SavedCheckin> findRecentByPlaceId(long placeId, Instant cutoff) {
        try {
            return jdbcTemplate.query(
                    SELECT_COLUMNS + """
                            WHERE place_id = ? AND created_at >= ?
                            ORDER BY created_at DESC, id DESC
                            """,
                    this::mapCheckin,
                    placeId,
                    Timestamp.from(cutoff)
            );
        } catch (DataAccessException exception) {
            throw new CheckinStorageException("Failed to load recent checkins", exception);
        }
    }

    private SavedCheckin findById(long id) {
        return jdbcTemplate.query(
                        SELECT_COLUMNS + "WHERE id = ?",
                        this::mapCheckin,
                        id
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new CheckinStorageException("Failed to load saved checkin"));
    }

    private SavedCheckin mapCheckin(ResultSet resultSet, int rowNum) throws SQLException {
        return new SavedCheckin(
                resultSet.getLong("id"),
                resultSet.getLong("place_id"),
                resultSet.getLong("user_id"),
                CrowdLevel.valueOf(resultSet.getString("crowd_level")),
                NoiseLevel.valueOf(resultSet.getString("noise_level")),
                resultSet.getBoolean("has_seat"),
                resultSet.getString("remark"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }
}
