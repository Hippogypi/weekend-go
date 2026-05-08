package com.weekendgo.auth;

public record AuthTokenResponse(
        String token,
        UserProfileResponse user
) {
}
