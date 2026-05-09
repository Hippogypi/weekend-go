package com.weekendgo.interaction;

import com.weekendgo.auth.AuthenticatedUser;
import com.weekendgo.place.Place;
import com.weekendgo.place.PlaceNotFoundException;
import com.weekendgo.place.PlaceRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final PlaceRepository placeRepository;

    public InteractionService(InteractionRepository interactionRepository, PlaceRepository placeRepository) {
        this.interactionRepository = interactionRepository;
        this.placeRepository = placeRepository;
    }

    public ReviewResponse createReview(long placeId, AuthenticatedUser user, ReviewRequest request) {
        requirePlace(placeId);
        return interactionRepository.createReview(placeId, user.account().id(), request);
    }

    public List<ReviewResponse> publicReviews(long placeId) {
        requirePlace(placeId);
        return interactionRepository.findApprovedReviews(placeId).stream()
                .map(ReviewResponse::publicView)
                .toList();
    }

    public ReviewResponse auditReview(long reviewId, AuthenticatedUser admin, AuditRequest request) {
        requireAuditableStatus(request.auditStatus());
        return interactionRepository.auditReview(reviewId, admin.account().id(), request.auditStatus(), request.reason())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
    }

    public ImageResponse createImage(long placeId, AuthenticatedUser user, ImageRequest request) {
        requirePlace(placeId);
        return interactionRepository.createImage(placeId, user.account().id(), request);
    }

    public List<ImageResponse> publicImages(long placeId) {
        requirePlace(placeId);
        return interactionRepository.findApprovedImages(placeId).stream()
                .map(ImageResponse::publicView)
                .toList();
    }

    public ImageResponse auditImage(long imageId, AuthenticatedUser admin, AuditRequest request) {
        requireAuditableStatus(request.auditStatus());
        return interactionRepository.auditImage(imageId, admin.account().id(), request.auditStatus(), request.reason())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
    }

    public FavoriteResponse favorite(long placeId, AuthenticatedUser user) {
        Place place = requirePlace(placeId);
        interactionRepository.favorite(user.account().id(), place);
        return new FavoriteResponse(placeId, true);
    }

    public FavoriteResponse unfavorite(long placeId, AuthenticatedUser user) {
        requirePlace(placeId);
        interactionRepository.unfavorite(user.account().id(), placeId);
        return new FavoriteResponse(placeId, false);
    }

    public FavoriteResponse favoriteStatus(long placeId, AuthenticatedUser user) {
        requirePlace(placeId);
        return new FavoriteResponse(placeId, interactionRepository.isFavorited(user.account().id(), placeId));
    }

    public List<FavoritePlaceResponse> favorites(AuthenticatedUser user) {
        return interactionRepository.findFavorites(user.account().id());
    }

    private Place requirePlace(long placeId) {
        return placeRepository.findById(placeId).orElseThrow(PlaceNotFoundException::new);
    }

    private void requireAuditableStatus(AuditStatus auditStatus) {
        if (auditStatus != AuditStatus.APPROVED && auditStatus != AuditStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audit status must be APPROVED or REJECTED");
        }
    }
}
