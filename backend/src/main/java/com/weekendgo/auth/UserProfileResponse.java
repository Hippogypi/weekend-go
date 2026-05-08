package com.weekendgo.auth;

public record UserProfileResponse(
        long id,
        String username,
        String role,
        String nickname
) {

    static UserProfileResponse from(UserAccount account) {
        return new UserProfileResponse(
                account.id(),
                account.username(),
                account.role().name(),
                account.nickname()
        );
    }
}
