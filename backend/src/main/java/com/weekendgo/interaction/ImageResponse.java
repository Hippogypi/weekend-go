package com.weekendgo.interaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImageResponse(
        long id,
        long placeId,
        long userId,
        String imageUrl,
        String description,
        AuditStatus auditStatus,
        Instant createdAt
) {
    public ImageResponse publicView() {
        return new ImageResponse(id, placeId, userId, imageUrl, description, null, createdAt);
    }
}
