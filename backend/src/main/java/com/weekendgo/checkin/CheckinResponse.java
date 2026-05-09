package com.weekendgo.checkin;

import java.time.Instant;

public record CheckinResponse(
        long id,
        long placeId,
        long userId,
        CrowdLevel crowdLevel,
        NoiseLevel noiseLevel,
        boolean hasSeat,
        String remark,
        Instant createdAt
) {

    static CheckinResponse from(SavedCheckin checkin) {
        return new CheckinResponse(
                checkin.id(),
                checkin.placeId(),
                checkin.userId(),
                checkin.crowdLevel(),
                checkin.noiseLevel(),
                checkin.hasSeat(),
                checkin.remark(),
                checkin.createdAt()
        );
    }
}
