package com.weekendgo.interaction;

import com.weekendgo.place.Place;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryInteractionRepository implements InteractionRepository {

    private final AtomicLong reviewIds = new AtomicLong(1);
    private final AtomicLong imageIds = new AtomicLong(1);
    private final Map<Long, ReviewResponse> reviews = new ConcurrentHashMap<>();
    private final Map<Long, ImageResponse> images = new ConcurrentHashMap<>();
    private final Map<FavoriteKey, FavoritePlaceResponse> favorites = new ConcurrentHashMap<>();

    @Override
    public ReviewResponse createReview(long placeId, long userId, ReviewRequest request) {
        ReviewResponse review = new ReviewResponse(
                reviewIds.getAndIncrement(),
                placeId,
                userId,
                request.quietScore(),
                request.wifiScore(),
                request.socketScore(),
                request.comfortScore(),
                request.costScore(),
                request.content(),
                AuditStatus.PENDING,
                Instant.now()
        );
        reviews.put(review.id(), review);
        return review;
    }

    @Override
    public List<ReviewResponse> findApprovedReviews(long placeId) {
        return reviews.values().stream()
                .filter(review -> review.placeId() == placeId)
                .filter(review -> review.auditStatus() == AuditStatus.APPROVED)
                .sorted(Comparator.comparing(ReviewResponse::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<ReviewResponse> auditReview(long reviewId, long adminId, AuditStatus auditStatus, String reason) {
        return Optional.ofNullable(reviews.computeIfPresent(reviewId, (id, review) -> new ReviewResponse(
                review.id(),
                review.placeId(),
                review.userId(),
                review.quietScore(),
                review.wifiScore(),
                review.socketScore(),
                review.comfortScore(),
                review.costScore(),
                review.content(),
                auditStatus,
                review.createdAt()
        )));
    }

    @Override
    public ImageResponse createImage(long placeId, long userId, ImageRequest request) {
        ImageResponse image = new ImageResponse(
                imageIds.getAndIncrement(),
                placeId,
                userId,
                request.imageUrl(),
                request.description(),
                AuditStatus.PENDING,
                Instant.now()
        );
        images.put(image.id(), image);
        return image;
    }

    @Override
    public List<ImageResponse> findApprovedImages(long placeId) {
        return images.values().stream()
                .filter(image -> image.placeId() == placeId)
                .filter(image -> image.auditStatus() == AuditStatus.APPROVED)
                .sorted(Comparator.comparing(ImageResponse::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<ImageResponse> auditImage(long imageId, long adminId, AuditStatus auditStatus, String reason) {
        return Optional.ofNullable(images.computeIfPresent(imageId, (id, image) -> new ImageResponse(
                image.id(),
                image.placeId(),
                image.userId(),
                image.imageUrl(),
                image.description(),
                auditStatus,
                image.createdAt()
        )));
    }

    @Override
    public void favorite(long userId, Place place) {
        favorites.putIfAbsent(
                new FavoriteKey(userId, place.id()),
                new FavoritePlaceResponse(place.id(), place.name(), Instant.now())
        );
    }

    @Override
    public void unfavorite(long userId, long placeId) {
        favorites.remove(new FavoriteKey(userId, placeId));
    }

    @Override
    public boolean isFavorited(long userId, long placeId) {
        return favorites.containsKey(new FavoriteKey(userId, placeId));
    }

    @Override
    public List<FavoritePlaceResponse> findFavorites(long userId) {
        return favorites.entrySet().stream()
                .filter(entry -> entry.getKey().userId() == userId)
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(FavoritePlaceResponse::createdAt).reversed())
                .toList();
    }

    private record FavoriteKey(long userId, long placeId) {
    }
}
