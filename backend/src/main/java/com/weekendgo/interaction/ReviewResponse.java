package com.weekendgo.interaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewResponse(
        long id,
        long placeId,
        long userId,
        BigDecimal quietScore,
        BigDecimal wifiScore,
        BigDecimal socketScore,
        BigDecimal comfortScore,
        BigDecimal costScore,
        String content,
        AuditStatus auditStatus,
        Instant createdAt
) {
    public ReviewResponse publicView() {
        return new ReviewResponse(
                id, placeId, userId, quietScore, wifiScore, socketScore,
                comfortScore, costScore, content, null, createdAt
        );
    }
}
