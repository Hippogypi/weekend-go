package com.weekendgo.profile;

import com.weekendgo.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspaceProfileController {

    private final WorkspaceProfileService workspaceProfileService;

    public WorkspaceProfileController(WorkspaceProfileService workspaceProfileService) {
        this.workspaceProfileService = workspaceProfileService;
    }

    @GetMapping("/api/places/{placeId}/workspace-profile")
    public ApiResponse<WorkspaceProfile> publicProfile(@PathVariable long placeId) {
        return ApiResponse.ok(workspaceProfileService.getPublicProfile(placeId));
    }
}
