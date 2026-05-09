package com.weekendgo.interaction;

import java.time.Instant;

public record FavoritePlaceResponse(
        long placeId,
        String placeName,
        Instant createdAt
) {
}
