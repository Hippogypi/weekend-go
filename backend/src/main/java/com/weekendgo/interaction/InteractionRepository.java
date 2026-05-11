package com.weekendgo.interaction;

import com.weekendgo.place.Place;
import java.util.List;
import java.util.Optional;

public interface InteractionRepository {

    ReviewResponse createReview(long placeId, long userId, ReviewRequest request);

    List<ReviewResponse> findApprovedReviews(long placeId);

    List<ReviewResponse> findApprovedReviews(long placeId, String sort);

    Optional<ReviewResponse> auditReview(long reviewId, long adminId, AuditStatus auditStatus, String reason);

    ImageResponse createImage(long placeId, long userId, ImageRequest request);

    ImageResponse saveImageWithReviewId(long placeId, long userId, long reviewId, String imageUrl, String description);

    List<ImageResponse> findImagesByReviewId(long reviewId);

    List<ImageResponse> findApprovedImages(long placeId);

    Optional<ImageResponse> auditImage(long imageId, long adminId, AuditStatus auditStatus, String reason);

    List<PendingAuditItem> findPendingReviews(int page, int size);

    List<PendingAuditItem> findPendingImages(int page, int size);

    long countPendingReviews();

    long countPendingImages();

    long countTodayApproved();

    long countTodayRejected();

    void favorite(long userId, Place place);

    void unfavorite(long userId, long placeId);

    boolean isFavorited(long userId, long placeId);

    List<FavoritePlaceResponse> findFavorites(long userId);

    List<ReviewResponse> findReviewsByUserId(long userId);

    void likeReview(long reviewId, long userId);

    void unlikeReview(long reviewId, long userId);

    boolean hasLiked(long reviewId, long userId);

    ReviewReply createReply(long reviewId, long userId, ReviewReplyRequest request);

    List<ReviewReply> findRepliesByReviewId(long reviewId);
}
