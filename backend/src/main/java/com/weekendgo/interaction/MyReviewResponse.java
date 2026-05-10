package com.weekendgo.interaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MyReviewResponse(
        long id,
        long placeId,
        String placeName,
        long userId,
        BigDecimal quietScore,
        BigDecimal wifiScore,
        BigDecimal socketScore,
        BigDecimal comfortScore,
        BigDecimal costScore,
        String content,
        AuditStatus auditStatus,
        Instant createdAt,
        List<ImageResponse> images
) {
}
