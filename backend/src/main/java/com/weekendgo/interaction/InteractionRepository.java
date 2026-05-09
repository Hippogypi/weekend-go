package com.weekendgo.interaction;

import com.weekendgo.place.Place;
import java.util.List;
import java.util.Optional;

public interface InteractionRepository {

    ReviewResponse createReview(long placeId, long userId, ReviewRequest request);

    List<ReviewResponse> findApprovedReviews(long placeId);

    Optional<ReviewResponse> auditReview(long reviewId, long adminId, AuditStatus auditStatus, String reason);

    ImageResponse createImage(long placeId, long userId, ImageRequest request);

    List<ImageResponse> findApprovedImages(long placeId);

    Optional<ImageResponse> auditImage(long imageId, long adminId, AuditStatus auditStatus, String reason);

    void favorite(long userId, Place place);

    void unfavorite(long userId, long placeId);

    boolean isFavorited(long userId, long placeId);

    List<FavoritePlaceResponse> findFavorites(long userId);
}
