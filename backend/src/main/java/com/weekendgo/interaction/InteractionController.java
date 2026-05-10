package com.weekendgo.interaction;

import com.weekendgo.auth.AuthenticatedUser;
import com.weekendgo.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping("/api/places/{placeId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> createReview(
            @PathVariable long placeId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.ok(interactionService.createReview(placeId, user, request));
    }

    @GetMapping("/api/places/{placeId}/reviews")
    public ApiResponse<List<ReviewResponse>> publicReviews(@PathVariable long placeId) {
        return ApiResponse.ok(interactionService.publicReviews(placeId));
    }

    @PostMapping("/api/places/{placeId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ImageResponse> createImage(
            @PathVariable long placeId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ImageRequest request
    ) {
        return ApiResponse.ok(interactionService.createImage(placeId, user, request));
    }

    @GetMapping("/api/places/{placeId}/images")
    public ApiResponse<List<ImageResponse>> publicImages(@PathVariable long placeId) {
        return ApiResponse.ok(interactionService.publicImages(placeId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/places/{placeId}/favorite")
    public ApiResponse<FavoriteResponse> favoriteStatus(
            @PathVariable long placeId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(interactionService.favoriteStatus(placeId, user));
    }

    @PostMapping("/api/places/{placeId}/favorite")
    public ApiResponse<FavoriteResponse> favorite(
            @PathVariable long placeId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(interactionService.favorite(placeId, user));
    }

    @DeleteMapping("/api/places/{placeId}/favorite")
    public ApiResponse<FavoriteResponse> unfavorite(
            @PathVariable long placeId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(interactionService.unfavorite(placeId, user));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/me/favorites")
    public ApiResponse<List<FavoritePlaceResponse>> favorites(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(interactionService.favorites(user));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/me/reviews")
    public ApiResponse<List<MyReviewResponse>> myReviews(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(interactionService.myReviews(user.account().id()));
    }

    @PatchMapping("/api/admin/reviews/{reviewId}/audit")
    public ApiResponse<ReviewResponse> auditReview(
            @PathVariable long reviewId,
            @AuthenticationPrincipal AuthenticatedUser admin,
            @Valid @RequestBody AuditRequest request
    ) {
        return ApiResponse.ok(interactionService.auditReview(reviewId, admin, request));
    }

    @PatchMapping("/api/admin/images/{imageId}/audit")
    public ApiResponse<ImageResponse> auditImage(
            @PathVariable long imageId,
            @AuthenticationPrincipal AuthenticatedUser admin,
            @Valid @RequestBody AuditRequest request
    ) {
        return ApiResponse.ok(interactionService.auditImage(imageId, admin, request));
    }
}
