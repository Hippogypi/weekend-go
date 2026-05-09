package com.weekendgo.checkin;

import com.weekendgo.auth.AuthenticatedUser;
import com.weekendgo.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping("/api/places/{placeId}/checkins")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckinResponse> create(
            @PathVariable long placeId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CheckinRequest request
    ) {
        return ApiResponse.ok(checkinService.create(placeId, user.account().id(), request));
    }

    @GetMapping("/api/places/{placeId}/current-status")
    public ApiResponse<CurrentStatusResponse> currentStatus(@PathVariable long placeId) {
        return ApiResponse.ok(checkinService.currentStatus(placeId));
    }
}
