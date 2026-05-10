package com.weekendgo.profile;

import com.weekendgo.auth.AuthenticatedUser;
import com.weekendgo.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class WorkspaceProfileController {

    private final WorkspaceProfileService workspaceProfileService;

    public WorkspaceProfileController(WorkspaceProfileService workspaceProfileService) {
        this.workspaceProfileService = workspaceProfileService;
    }

    @PostMapping("/api/places/{placeId}/profile-submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProfileSubmission> submit(
            @PathVariable long placeId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ProfileSubmissionRequest request
    ) {
        return ApiResponse.ok(workspaceProfileService.submit(placeId, user.account().id(), request));
    }

    @PostMapping("/api/admin/profile-submissions/{submissionId}/approve")
    public ApiResponse<ProfileSubmission> approve(
            @PathVariable long submissionId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody(required = false) AuditRequest request
    ) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok(workspaceProfileService.approve(submissionId, user.account().id(), reason));
    }

    @PostMapping("/api/admin/profile-submissions/{submissionId}/reject")
    public ApiResponse<ProfileSubmission> reject(
            @PathVariable long submissionId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AuditRequest request
    ) {
        return ApiResponse.ok(workspaceProfileService.reject(submissionId, user.account().id(), request.reason()));
    }

    @GetMapping("/api/places/{placeId}/workspace-profile")
    public ApiResponse<WorkspaceProfile> publicProfile(@PathVariable long placeId) {
        return ApiResponse.ok(workspaceProfileService.getPublicProfile(placeId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/me/profile-submissions")
    public ApiResponse<List<MyProfileSubmissionResponse>> myProfileSubmissions(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(workspaceProfileService.mySubmissions(user.account().id()));
    }
}
