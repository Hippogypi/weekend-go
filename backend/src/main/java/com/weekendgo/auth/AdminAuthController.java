package com.weekendgo.auth;

import com.weekendgo.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    @GetMapping("/auth-check")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserProfileResponse> authCheck(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(UserProfileResponse.from(principal.account()));
    }
}
