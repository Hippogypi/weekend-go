package com.weekendgo.checkin;

import java.time.Instant;

public record MyCheckinResponse(
        long id,
        long placeId,
        String placeName,
        long userId,
        CrowdLevel crowdLevel,
        NoiseLevel noiseLevel,
        boolean hasSeat,
        String remark,
        Instant createdAt
) {
}
