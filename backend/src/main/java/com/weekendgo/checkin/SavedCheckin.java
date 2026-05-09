package com.weekendgo.checkin;

import java.time.Instant;

public record SavedCheckin(
        long id,
        long placeId,
        long userId,
        CrowdLevel crowdLevel,
        NoiseLevel noiseLevel,
        boolean hasSeat,
        String remark,
        Instant createdAt
) {
}
