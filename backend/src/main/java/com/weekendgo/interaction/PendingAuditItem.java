package com.weekendgo.interaction;

import java.time.Instant;

public record PendingAuditItem(
        long id,
        long placeId,
        String placeName,
        long userId,
        String username,
        String content,
        Instant createdAt,
        String type
) {
}
