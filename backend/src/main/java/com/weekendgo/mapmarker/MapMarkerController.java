package com.weekendgo.mapmarker;

import com.weekendgo.auth.AuthenticatedUser;
import com.weekendgo.auth.UserRole;
import com.weekendgo.common.api.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class MapMarkerController {

    private final MapMarkerRepository mapMarkerRepository;

    public MapMarkerController(MapMarkerRepository mapMarkerRepository) {
        this.mapMarkerRepository = mapMarkerRepository;
    }

    @GetMapping("/api/map/markers")
    public ApiResponse<List<MapMarkerResponse>> markers(
            @RequestParam("longitude") BigDecimal longitude,
            @RequestParam("latitude") BigDecimal latitude,
            @RequestParam(value = "radius", defaultValue = "5000") @Min(1) @Max(50000) int radius,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long userId = user != null && user.account().role() == UserRole.USER ? user.account().id() : null;
        return ApiResponse.ok(mapMarkerRepository.findNearbyMarkers(longitude, latitude, radius, userId));
    }
}
