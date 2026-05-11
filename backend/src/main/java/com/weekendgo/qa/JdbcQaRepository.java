package com.weekendgo.qa;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class JdbcQaRepository implements QaRepository {

    private static final String INSERT_QUESTION_SQL = """
            INSERT INTO place_qa (place_id, user_id, type, content)
            VALUES (?, ?, 'QUESTION', ?)
            """;

    private static final String INSERT_ANSWER_SQL = """
            INSERT INTO place_qa (place_id, user_id, type, parent_id, content)
            VALUES (?, ?, 'ANSWER', ?, ?)
            """;

    private static final String UPDATE_ANSWER_COUNT_SQL = """
            UPDATE place_qa SET answer_count = answer_count + 1 WHERE id = ?
            """;

    private static final String SELECT_COLUMNS = """
            SELECT id, place_id, user_id, type, parent_id, content, answer_count, created_at
            FROM place_qa
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JdbcQaRepository(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public PlaceQa createQuestion(long placeId, long userId, QuestionRequest request) {
        try {
            PlaceQa question = transactionTemplate.execute(status -> {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            INSERT_QUESTION_SQL, new String[]{"id"});
                    statement.setLong(1, placeId);
                    statement.setLong(2, userId);
                    statement.setString(3, request.content());
                    return statement;
                }, keyHolder);
                return findById(requiredKey(keyHolder));
            });
            if (question == null) {
                throw new QaStorageException("Failed to persist question");
            }
            return question;
        } catch (DataAccessException exception) {
            throw new QaStorageException("Failed to persist question", exception);
        }
    }

    @Override
    public PlaceQa createAnswer(long questionId, long userId, AnswerRequest request) {
        try {
            PlaceQa answer = transactionTemplate.execute(status -> {
                Long placeId;
                try {
                    placeId = jdbcTemplate.queryForObject(
                            "SELECT place_id FROM place_qa WHERE id = ? AND type = 'QUESTION'",
                            Long.class,
                            questionId
                    );
                } catch (EmptyResultDataAccessException e) {
                    throw new QuestionNotFoundException();
                }
                if (placeId == null) {
                    throw new QuestionNotFoundException();
                }

                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            INSERT_ANSWER_SQL, new String[]{"id"});
                    statement.setLong(1, placeId);
                    statement.setLong(2, userId);
                    statement.setLong(3, questionId);
                    statement.setString(4, request.content());
                    return statement;
                }, keyHolder);

                jdbcTemplate.update(UPDATE_ANSWER_COUNT_SQL, questionId);

                return findById(requiredKey(keyHolder));
            });
            if (answer == null) {
                throw new QaStorageException("Failed to persist answer");
            }
            return answer;
        } catch (DataAccessException exception) {
            throw new QaStorageException("Failed to persist answer", exception);
        }
    }

    @Override
    public List<PlaceQa> findQuestionsByPlaceId(long placeId) {
        try {
            return jdbcTemplate.query(
                    SELECT_COLUMNS + "WHERE place_id = ? AND type = 'QUESTION' ORDER BY created_at DESC",
                    this::mapPlaceQa,
                    placeId
            );
        } catch (DataAccessException exception) {
            throw new QaStorageException("Failed to load questions", exception);
        }
    }

    @Override
    public List<PlaceQa> findAnswersByQuestionId(long questionId) {
        try {
            return jdbcTemplate.query(
                    SELECT_COLUMNS + "WHERE parent_id = ? AND type = 'ANSWER' ORDER BY created_at ASC",
                    this::mapPlaceQa,
                    questionId
            );
        } catch (DataAccessException exception) {
            throw new QaStorageException("Failed to load answers", exception);
        }
    }

    private PlaceQa findById(long id) {
        return jdbcTemplate.query(
                        SELECT_COLUMNS + "WHERE id = ?",
                        this::mapPlaceQa,
                        id
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new QaStorageException("Failed to load saved qa"));
    }

    private long requiredKey(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to retrieve generated id");
        }
        return key.longValue();
    }

    private PlaceQa mapPlaceQa(ResultSet resultSet, int rowNum) throws SQLException {
        long parentIdRaw = resultSet.getLong("parent_id");
        Long parentId = resultSet.wasNull() ? null : parentIdRaw;
        return new PlaceQa(
                resultSet.getLong("id"),
                resultSet.getLong("place_id"),
                resultSet.getLong("user_id"),
                resultSet.getString("type"),
                parentId,
                resultSet.getString("content"),
                resultSet.getInt("answer_count"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }
}
