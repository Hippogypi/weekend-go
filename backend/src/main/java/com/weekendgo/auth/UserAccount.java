package com.weekendgo.auth;

import java.time.Instant;

public record UserAccount(
        long id,
        String username,
        String passwordHash,
        UserRole role,
        String nickname,
        boolean enabled,
        Instant createdAt
) {
}
