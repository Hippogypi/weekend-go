package com.weekendgo.place;

public record PlaceLinksResponse(
        String detail,
        String profileContributions,
        String checkins,
        String reviews,
        String images
) {

    public static PlaceLinksResponse forPlace(long placeId) {
        String basePath = "/api/places/" + placeId;
        return new PlaceLinksResponse(
                basePath,
                basePath + "/profile-submissions",
                basePath + "/checkins",
                basePath + "/reviews",
                basePath + "/images"
        );
    }
}
