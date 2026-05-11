package com.weekendgo.qa;

import java.time.Instant;

public record PlaceQa(
    long id,
    long placeId,
    long userId,
    String type,
    Long parentId,
    String content,
    int answerCount,
    Instant createdAt
) {
}
