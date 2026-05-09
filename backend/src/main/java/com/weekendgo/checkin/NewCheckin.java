package com.weekendgo.checkin;

import java.time.Instant;

public record NewCheckin(
        long placeId,
        long userId,
        CrowdLevel crowdLevel,
        NoiseLevel noiseLevel,
        boolean hasSeat,
        String remark,
        Instant createdAt
) {
}
