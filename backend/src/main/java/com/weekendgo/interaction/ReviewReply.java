package com.weekendgo.interaction;

import java.time.Instant;

public record ReviewReply(
    long id,
    long reviewId,
    long userId,
    String content,
    Instant createdAt
) {}
