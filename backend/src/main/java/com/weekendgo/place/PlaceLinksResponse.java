package com.weekendgo.place;

public record PlaceLinksResponse(
        String detail,
        String checkins,
        String reviews,
        String images
) {

    public static PlaceLinksResponse forPlace(long placeId) {
        String basePath = "/api/places/" + placeId;
        return new PlaceLinksResponse(
                basePath,
                basePath + "/checkins",
                basePath + "/reviews",
                basePath + "/images"
        );
    }
}
