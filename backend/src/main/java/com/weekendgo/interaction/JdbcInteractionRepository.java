package com.weekendgo.interaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weekendgo.place.Place;
import com.weekendgo.profile.AllowLongStay;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class JdbcInteractionRepository implements InteractionRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private static final String REVIEW_COLUMNS = """
            SELECT id, place_id, user_id, quiet_score, wifi_score, socket_score,
                   comfort_score, cost_score, content, audit_status, created_at,
                   seat_score, min_consumption, allow_long_stay, suitable_scenes,
                   like_count, reply_count
            FROM reviews
            """;

    private static final String IMAGE_COLUMNS = """
            SELECT id, place_id, user_id, image_url, description, audit_status, created_at
            FROM place_images
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public JdbcInteractionRepository(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReviewResponse createReview(long placeId, long userId, ReviewRequest request) {
        try {
            ReviewResponse review = transactionTemplate.execute(status -> {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO reviews (
                              place_id, user_id, quiet_score, wifi_score, socket_score,
                              comfort_score, cost_score, content, seat_score,
                              min_consumption, allow_long_stay, suitable_scenes, audit_status
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                            """, new String[]{"id"});
                    statement.setLong(1, placeId);
                    statement.setLong(2, userId);
                    statement.setBigDecimal(3, request.quietScore());
                    statement.setBigDecimal(4, request.wifiScore());
                    statement.setBigDecimal(5, request.socketScore());
                    statement.setBigDecimal(6, request.comfortScore());
                    statement.setBigDecimal(7, request.costScore());
                    statement.setString(8, request.content());
                    if (request.seatScore() == null) {
                        statement.setObject(9, null);
                    } else {
                        statement.setBigDecimal(9, request.seatScore());
                    }
                    if (request.minConsumption() == null) {
                        statement.setObject(10, null);
                    } else {
                        statement.setInt(10, request.minConsumption());
                    }
                    String allowLongStay = request.allowLongStay() == null ? "UNKNOWN" : request.allowLongStay();
                    statement.setString(11, allowLongStay);
                    statement.setString(12, writeScenes(request.suitableScenes()));
                    return statement;
                }, keyHolder);
                return findReviewById(requiredKey(keyHolder));
            });
            return review == null ? findReviewByUserLatest(userId) : review;
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to create review", exception);
        }
    }

    @Override
    public List<ReviewResponse> findApprovedReviews(long placeId) {
        return findApprovedReviews(placeId, "time");
    }

    @Override
    public List<ReviewResponse> findApprovedReviews(long placeId, String sort) {
        try {
            String orderBy = "hot".equalsIgnoreCase(sort)
                    ? "r.like_count DESC, r.created_at DESC, r.id DESC, i.created_at DESC"
                    : "r.created_at DESC, r.id DESC, i.created_at DESC";
            String sql = """
                    SELECT r.id, r.place_id, r.user_id, r.quiet_score, r.wifi_score, r.socket_score,
                           r.comfort_score, r.cost_score, r.content, r.audit_status, r.created_at,
                           r.seat_score, r.min_consumption, r.allow_long_stay, r.suitable_scenes,
                           r.like_count, r.reply_count,
                           i.id as image_id, i.user_id as image_user_id,
                           i.image_url, i.description, i.created_at as image_created_at
                    FROM reviews r
                    LEFT JOIN place_images i ON i.review_id = r.id AND i.audit_status = 'APPROVED'
                    WHERE r.place_id = ? AND r.audit_status = 'APPROVED'
                    ORDER BY """ + " " + orderBy;
            return jdbcTemplate.query(sql, this::extractReviews, placeId);
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to load reviews", exception);
        }
    }

    @Override
    public List<ReviewResponse> findReviewsByUserId(long userId) {
        try {
            return jdbcTemplate.query("""
                    SELECT r.id, r.place_id, r.user_id, r.quiet_score, r.wifi_score, r.socket_score,
                           r.comfort_score, r.cost_score, r.content, r.audit_status, r.created_at,
                           r.seat_score, r.min_consumption, r.allow_long_stay, r.suitable_scenes,
                           r.like_count, r.reply_count,
                           i.id as image_id, i.user_id as image_user_id,
                           i.image_url, i.description, i.audit_status as image_audit_status, i.created_at as image_created_at
                    FROM reviews r
                    LEFT JOIN place_images i ON i.review_id = r.id
                    WHERE r.user_id = ?
                    ORDER BY r.created_at DESC, r.id DESC, i.created_at DESC
                    """, (ResultSet rs) -> {
                Map<Long, ReviewAccumulator> map = new LinkedHashMap<>();
                while (rs.next()) {
                    long reviewId = rs.getLong("id");
                    ReviewAccumulator acc = map.get(reviewId);
                    if (acc == null) {
                        acc = new ReviewAccumulator(
                                reviewId,
                                rs.getLong("place_id"),
                                rs.getLong("user_id"),
                                rs.getBigDecimal("quiet_score"),
                                rs.getBigDecimal("wifi_score"),
                                rs.getBigDecimal("socket_score"),
                                rs.getBigDecimal("comfort_score"),
                                rs.getBigDecimal("cost_score"),
                                rs.getString("content"),
                                AuditStatus.valueOf(rs.getString("audit_status")),
                                toInstant(rs.getTimestamp("created_at")),
                                rs.getBigDecimal("seat_score"),
                                nullableInt(rs, "min_consumption"),
                                nullableAllowLongStay(rs, "allow_long_stay"),
                                readScenes(rs.getString("suitable_scenes")),
                                rs.getInt("like_count"),
                                rs.getInt("reply_count")
                        );
                        map.put(reviewId, acc);
                    }
                    long imageId = rs.getLong("image_id");
                    if (!rs.wasNull()) {
                        acc.addImage(new ImageResponse(
                                imageId,
                                rs.getLong("place_id"),
                                rs.getLong("image_user_id"),
                                rs.getString("image_url"),
                                rs.getString("description"),
                                AuditStatus.valueOf(rs.getString("image_audit_status")),
                                toInstant(rs.getTimestamp("image_created_at"))
                        ));
                    }
                }
                return map.values().stream().map(ReviewAccumulator::toResponse).toList();
            }, userId);
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to load reviews", exception);
        }
    }

    private List<ReviewResponse> extractReviews(ResultSet rs) throws SQLException {
        Map<Long, ReviewAccumulator> map = new LinkedHashMap<>();
        while (rs.next()) {
            long reviewId = rs.getLong("id");
            ReviewAccumulator acc = map.get(reviewId);
            if (acc == null) {
                acc = new ReviewAccumulator(
                        reviewId,
                        rs.getLong("place_id"),
                        rs.getLong("user_id"),
                        rs.getBigDecimal("quiet_score"),
                        rs.getBigDecimal("wifi_score"),
                        rs.getBigDecimal("socket_score"),
                        rs.getBigDecimal("comfort_score"),
                        rs.getBigDecimal("cost_score"),
                        rs.getString("content"),
                        AuditStatus.valueOf(rs.getString("audit_status")),
                        toInstant(rs.getTimestamp("created_at")),
                        rs.getBigDecimal("seat_score"),
                        nullableInt(rs, "min_consumption"),
                        nullableAllowLongStay(rs, "allow_long_stay"),
                        readScenes(rs.getString("suitable_scenes")),
                        rs.getInt("like_count"),
                        rs.getInt("reply_count")
                );
                map.put(reviewId, acc);
            }
            long imageId = rs.getLong("image_id");
            if (!rs.wasNull()) {
                acc.addImage(new ImageResponse(
                        imageId,
                        rs.getLong("place_id"),
                        rs.getLong("image_user_id"),
                        rs.getString("image_url"),
                        rs.getString("description"),
                        AuditStatus.APPROVED,
                        toInstant(rs.getTimestamp("image_created_at"))
                ));
            }
        }
        return map.values().stream().map(ReviewAccumulator::toResponse).toList();
    }

    @Override
    public Optional<ReviewResponse> auditReview(long reviewId, long adminId, AuditStatus auditStatus, String reason) {
        try {
            return Optional.ofNullable(transactionTemplate.execute(status -> {
                int updated = jdbcTemplate.update("""
                        UPDATE reviews
                        SET audit_status = ?, audited_by = ?, audited_at = CURRENT_TIMESTAMP, audit_reason = ?
                        WHERE id = ?
                        """, auditStatus.name(), adminId, reason, reviewId);
                if (updated == 0) {
                    return null;
                }
                insertAuditLog("REVIEW", reviewId, adminId, auditStatus, reason);
                return findReviewById(reviewId);
            }));
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to audit review", exception);
        }
    }

    @Override
    public ImageResponse createImage(long placeId, long userId, ImageRequest request) {
        try {
            ImageResponse image = transactionTemplate.execute(status -> {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO place_images (place_id, user_id, image_url, description, audit_status)
                            VALUES (?, ?, ?, ?, 'PENDING')
                            """, new String[]{"id"});
                    statement.setLong(1, placeId);
                    statement.setLong(2, userId);
                    statement.setString(3, request.imageUrl());
                    statement.setString(4, request.description());
                    return statement;
                }, keyHolder);
                return findImageById(requiredKey(keyHolder));
            });
            return image == null ? findImageByUserLatest(userId) : image;
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to create image", exception);
        }
    }

    @Override
    public ImageResponse saveImageWithReviewId(long placeId, long userId, long reviewId, String imageUrl, String description) {
        try {
            ImageResponse image = transactionTemplate.execute(status -> {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO place_images (place_id, user_id, review_id, image_url, description, audit_status)
                            VALUES (?, ?, ?, ?, ?, 'PENDING')
                            """, new String[]{"id"});
                    statement.setLong(1, placeId);
                    statement.setLong(2, userId);
                    statement.setLong(3, reviewId);
                    statement.setString(4, imageUrl);
                    statement.setString(5, description);
                    return statement;
                }, keyHolder);
                return findImageById(requiredKey(keyHolder));
            });
            return image == null ? findImageByUserLatest(userId) : image;
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to create image with review", exception);
        }
    }

    @Override
    public List<ImageResponse> findImagesByReviewId(long reviewId) {
        try {
            return jdbcTemplate.query(
                    IMAGE_COLUMNS + "WHERE review_id = ? ORDER BY created_at DESC, id DESC",
                    this::mapImage,
                    reviewId
            );
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to load images by review", exception);
        }
    }

    @Override
    public List<ImageResponse> findApprovedImages(long placeId) {
        try {
            return jdbcTemplate.query(
                    IMAGE_COLUMNS + "WHERE place_id = ? AND audit_status = 'APPROVED' ORDER BY created_at DESC, id DESC",
                    this::mapImage,
                    placeId
            );
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to load images", exception);
        }
    }

    @Override
    public Optional<ImageResponse> auditImage(long imageId, long adminId, AuditStatus auditStatus, String reason) {
        try {
            return Optional.ofNullable(transactionTemplate.execute(status -> {
                int updated = jdbcTemplate.update("""
                        UPDATE place_images
                        SET audit_status = ?, audited_by = ?, audited_at = CURRENT_TIMESTAMP, audit_reason = ?
                        WHERE id = ?
                        """, auditStatus.name(), adminId, reason, imageId);
                if (updated == 0) {
                    return null;
                }
                insertAuditLog("PLACE_IMAGE", imageId, adminId, auditStatus, reason);
                return findImageById(imageId);
            }));
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to audit image", exception);
        }
    }

    @Override
    public void favorite(long userId, Place place) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO favorites (user_id, place_id)
                    VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE created_at = created_at
                    """, userId, place.id());
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to favorite place", exception);
        }
    }

    @Override
    public void unfavorite(long userId, long placeId) {
        try {
            jdbcTemplate.update("DELETE FROM favorites WHERE user_id = ? AND place_id = ?", userId, placeId);
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to unfavorite place", exception);
        }
    }

    @Override
    public boolean isFavorited(long userId, long placeId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM favorites WHERE user_id = ? AND place_id = ?",
                    Integer.class,
                    userId,
                    placeId
            );
            return count != null && count > 0;
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to load favorite status", exception);
        }
    }

    @Override
    public List<PendingAuditItem> findPendingReviews(int page, int size) {
        try {
            return jdbcTemplate.query("""
                    SELECT r.id, r.place_id, p.name AS place_name, r.user_id, u.username, r.content, r.created_at
                    FROM reviews r
                    JOIN places p ON p.id = r.place_id
                    JOIN users u ON u.id = r.user_id
                    WHERE r.audit_status = 'PENDING'
                    ORDER BY r.created_at DESC, r.id DESC
                    LIMIT ? OFFSET ?
                    """, pendingAuditItemMapper("review"), size, (page - 1) * size);
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to load pending reviews", exception);
        }
    }

    @Override
    public List<PendingAuditItem> findPendingImages(int page, int size) {
        try {
            return jdbcTemplate.query("""
                    SELECT i.id, i.place_id, p.name AS place_name, i.user_id, u.username,
                           COALESCE(NULLIF(i.description, ''), i.image_url) AS content, i.created_at
                    FROM place_images i
                    JOIN places p ON p.id = i.place_id
                    JOIN users u ON u.id = i.user_id
                    WHERE i.audit_status = 'PENDING'
                    ORDER BY i.created_at DESC, i.id DESC
                    LIMIT ? OFFSET ?
                    """, pendingAuditItemMapper("image"), size, (page - 1) * size);
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to load pending images", exception);
        }
    }

    @Override
    public long countPendingReviews() {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reviews WHERE audit_status = 'PENDING'", Long.class);
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to count pending reviews", exception);
        }
    }

    @Override
    public long countPendingImages() {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM place_images WHERE audit_status = 'PENDING'", Long.class);
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to count pending images", exception);
        }
    }

    @Override
    public long countTodayApproved() {
        try {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM audit_logs WHERE action = 'APPROVED' AND created_at >= CURRENT_DATE
                    """, Long.class);
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to count today approved", exception);
        }
    }

    @Override
    public long countTodayRejected() {
        try {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM audit_logs WHERE action = 'REJECTED' AND created_at >= CURRENT_DATE
                    """, Long.class);
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to count today rejected", exception);
        }
    }

    @Override
    public List<FavoritePlaceResponse> findFavorites(long userId) {
        try {
            return jdbcTemplate.query("""
                    SELECT f.place_id, p.name AS place_name, f.created_at
                    FROM favorites f
                    JOIN places p ON p.id = f.place_id
                    WHERE f.user_id = ?
                    ORDER BY f.created_at DESC, f.id DESC
                    """, (resultSet, rowNum) -> new FavoritePlaceResponse(
                    resultSet.getLong("place_id"),
                    resultSet.getString("place_name"),
                    toInstant(resultSet.getTimestamp("created_at"))
            ), userId);
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to load favorites", exception);
        }
    }

    private ReviewResponse findReviewById(long id) {
        return jdbcTemplate.query(REVIEW_COLUMNS + "WHERE id = ?", this::mapReview, id).stream()
                .findFirst()
                .orElseThrow();
    }

    private ReviewResponse findReviewByUserLatest(long userId) {
        return jdbcTemplate.query(
                        REVIEW_COLUMNS + "WHERE user_id = ? ORDER BY id DESC LIMIT 1",
                        this::mapReview,
                        userId
                ).stream()
                .findFirst()
                .orElseThrow();
    }

    private ImageResponse findImageById(long id) {
        return jdbcTemplate.query(IMAGE_COLUMNS + "WHERE id = ?", this::mapImage, id).stream()
                .findFirst()
                .orElseThrow();
    }

    private ImageResponse findImageByUserLatest(long userId) {
        return jdbcTemplate.query(
                        IMAGE_COLUMNS + "WHERE user_id = ? ORDER BY id DESC LIMIT 1",
                        this::mapImage,
                        userId
                ).stream()
                .findFirst()
                .orElseThrow();
    }

    private void insertAuditLog(String targetType, long targetId, long adminId, AuditStatus auditStatus, String reason) {
        jdbcTemplate.update("""
                INSERT INTO audit_logs (target_type, target_id, admin_id, action, reason)
                VALUES (?, ?, ?, ?, ?)
                """, targetType, targetId, adminId, auditStatus.name(), reason);
    }

    private long requiredKey(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to retrieve generated id");
        }
        return key.longValue();
    }

    @Override
    public void likeReview(long reviewId, long userId) {
        try {
            int updated = jdbcTemplate.update("""
                    INSERT IGNORE INTO review_likes (review_id, user_id)
                    VALUES (?, ?)
                    """, reviewId, userId);
            if (updated > 0) {
                jdbcTemplate.update("""
                        UPDATE reviews SET like_count = like_count + 1 WHERE id = ?
                        """, reviewId);
            }
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to like review", exception);
        }
    }

    @Override
    public void unlikeReview(long reviewId, long userId) {
        try {
            int deleted = jdbcTemplate.update("DELETE FROM review_likes WHERE review_id = ? AND user_id = ?", reviewId, userId);
            if (deleted > 0) {
                jdbcTemplate.update("""
                        UPDATE reviews SET like_count = GREATEST(like_count - 1, 0) WHERE id = ?
                        """, reviewId);
            }
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to unlike review", exception);
        }
    }

    @Override
    public boolean hasLiked(long reviewId, long userId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM review_likes WHERE review_id = ? AND user_id = ?",
                    Integer.class,
                    reviewId,
                    userId
            );
            return count != null && count > 0;
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to load like status", exception);
        }
    }

    @Override
    public ReviewReply createReply(long reviewId, long userId, ReviewReplyRequest request) {
        try {
            return transactionTemplate.execute(status -> {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO review_replies (review_id, user_id, content)
                            VALUES (?, ?, ?)
                            """, new String[]{"id"});
                    statement.setLong(1, reviewId);
                    statement.setLong(2, userId);
                    statement.setString(3, request.content());
                    return statement;
                }, keyHolder);
                jdbcTemplate.update("""
                        UPDATE reviews SET reply_count = reply_count + 1 WHERE id = ?
                        """, reviewId);
                return findReplyById(requiredKey(keyHolder));
            });
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to create reply", exception);
        }
    }

    @Override
    public List<ReviewReply> findRepliesByReviewId(long reviewId) {
        try {
            return jdbcTemplate.query("""
                    SELECT id, review_id, user_id, content, created_at
                    FROM review_replies
                    WHERE review_id = ?
                    ORDER BY created_at ASC
                    """, (rs, rowNum) -> new ReviewReply(
                    rs.getLong("id"),
                    rs.getLong("review_id"),
                    rs.getLong("user_id"),
                    rs.getString("content"),
                    toInstant(rs.getTimestamp("created_at"))
            ), reviewId);
        } catch (DataAccessException exception) {
            throw new InteractionStorageException("Failed to load replies", exception);
        }
    }

    private ReviewReply findReplyById(long id) {
        return jdbcTemplate.query("""
                SELECT id, review_id, user_id, content, created_at
                FROM review_replies
                WHERE id = ?
                """, (rs, rowNum) -> new ReviewReply(
                rs.getLong("id"),
                rs.getLong("review_id"),
                rs.getLong("user_id"),
                rs.getString("content"),
                toInstant(rs.getTimestamp("created_at"))
        ), id).stream().findFirst().orElseThrow();
    }

    private ReviewResponse mapReview(ResultSet resultSet, int rowNum) throws SQLException {
        return new ReviewResponse(
                resultSet.getLong("id"),
                resultSet.getLong("place_id"),
                resultSet.getLong("user_id"),
                resultSet.getBigDecimal("quiet_score"),
                resultSet.getBigDecimal("wifi_score"),
                resultSet.getBigDecimal("socket_score"),
                resultSet.getBigDecimal("comfort_score"),
                resultSet.getBigDecimal("cost_score"),
                resultSet.getString("content"),
                AuditStatus.valueOf(resultSet.getString("audit_status")),
                toInstant(resultSet.getTimestamp("created_at")),
                null,
                resultSet.getBigDecimal("seat_score"),
                nullableInt(resultSet, "min_consumption"),
                nullableAllowLongStay(resultSet, "allow_long_stay"),
                readScenes(resultSet.getString("suitable_scenes")),
                resultSet.getInt("like_count"),
                resultSet.getInt("reply_count"),
                null
        );
    }

    private ImageResponse mapImage(ResultSet resultSet, int rowNum) throws SQLException {
        return new ImageResponse(
                resultSet.getLong("id"),
                resultSet.getLong("place_id"),
                resultSet.getLong("user_id"),
                resultSet.getString("image_url"),
                resultSet.getString("description"),
                AuditStatus.valueOf(resultSet.getString("audit_status")),
                toInstant(resultSet.getTimestamp("created_at"))
        );
    }

    private org.springframework.jdbc.core.RowMapper<PendingAuditItem> pendingAuditItemMapper(String type) {
        return (resultSet, rowNum) -> new PendingAuditItem(
                resultSet.getLong("id"),
                resultSet.getLong("place_id"),
                resultSet.getString("place_name"),
                resultSet.getLong("user_id"),
                resultSet.getString("username"),
                resultSet.getString("content"),
                toInstant(resultSet.getTimestamp("created_at")),
                type
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private AllowLongStay nullableAllowLongStay(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : AllowLongStay.valueOf(value);
    }

    private String writeScenes(List<String> scenes) {
        if (scenes == null || scenes.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(scenes);
        } catch (JsonProcessingException exception) {
            throw new InteractionStorageException("Failed to serialize suitable scenes", exception);
        }
    }

    private List<String> readScenes(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            if (raw.startsWith("\"")) {
                raw = objectMapper.readValue(raw, String.class);
            }
            return objectMapper.readValue(raw, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new InteractionStorageException("Failed to deserialize suitable scenes", exception);
        }
    }

    private static class ReviewAccumulator {

        private final long id;
        private final long placeId;
        private final long userId;
        private final BigDecimal quietScore;
        private final BigDecimal wifiScore;
        private final BigDecimal socketScore;
        private final BigDecimal comfortScore;
        private final BigDecimal costScore;
        private final String content;
        private final AuditStatus auditStatus;
        private final Instant createdAt;
        private final List<ImageResponse> images = new ArrayList<>();
        private final BigDecimal seatScore;
        private final Integer minConsumption;
        private final AllowLongStay allowLongStay;
        private final List<String> suitableScenes;
        private final int likeCount;
        private final int replyCount;

        ReviewAccumulator(long id, long placeId, long userId,
                          BigDecimal quietScore, BigDecimal wifiScore, BigDecimal socketScore,
                          BigDecimal comfortScore, BigDecimal costScore, String content,
                          AuditStatus auditStatus, Instant createdAt,
                          BigDecimal seatScore, Integer minConsumption, AllowLongStay allowLongStay,
                          List<String> suitableScenes, int likeCount, int replyCount) {
            this.id = id;
            this.placeId = placeId;
            this.userId = userId;
            this.quietScore = quietScore;
            this.wifiScore = wifiScore;
            this.socketScore = socketScore;
            this.comfortScore = comfortScore;
            this.costScore = costScore;
            this.content = content;
            this.auditStatus = auditStatus;
            this.createdAt = createdAt;
            this.seatScore = seatScore;
            this.minConsumption = minConsumption;
            this.allowLongStay = allowLongStay;
            this.suitableScenes = suitableScenes;
            this.likeCount = likeCount;
            this.replyCount = replyCount;
        }

        void addImage(ImageResponse image) {
            images.add(image);
        }

        ReviewResponse toResponse() {
            return new ReviewResponse(
                    id, placeId, userId, quietScore, wifiScore, socketScore,
                    comfortScore, costScore, content, auditStatus, createdAt, List.copyOf(images),
                    seatScore, minConsumption, allowLongStay, suitableScenes, likeCount, replyCount,
                    null
            );
        }
    }
}
