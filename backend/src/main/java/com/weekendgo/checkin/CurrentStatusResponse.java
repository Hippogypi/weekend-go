package com.weekendgo.checkin;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CurrentStatusResponse(
        long placeId,
        CurrentStatusType status,
        String message,
        int sampleCount,
        Instant since,
        CrowdLevel crowdLevel,
        NoiseLevel noiseLevel,
        Boolean hasSeat,
        BigDecimal seatAvailabilityRatio
) {

    static CurrentStatusResponse empty(long placeId, Instant since) {
        return new CurrentStatusResponse(
                placeId,
                CurrentStatusType.NO_RECENT_FEEDBACK,
                "暂无近期反馈",
                0,
                since,
                null,
                null,
                null,
                null
        );
    }

    static CurrentStatusResponse active(
            long placeId,
            Instant since,
            int sampleCount,
            CrowdLevel crowdLevel,
            NoiseLevel noiseLevel,
            boolean hasSeat,
            BigDecimal seatAvailabilityRatio
    ) {
        return new CurrentStatusResponse(
                placeId,
                CurrentStatusType.ACTIVE,
                "近期反馈已聚合",
                sampleCount,
                since,
                crowdLevel,
                noiseLevel,
                hasSeat,
                seatAvailabilityRatio
        );
    }
}
