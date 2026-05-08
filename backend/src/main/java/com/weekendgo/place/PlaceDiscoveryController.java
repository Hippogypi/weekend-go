package com.weekendgo.place;

import com.weekendgo.common.api.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class PlaceDiscoveryController {

    private final PlaceDiscoveryService placeDiscoveryService;

    public PlaceDiscoveryController(PlaceDiscoveryService placeDiscoveryService) {
        this.placeDiscoveryService = placeDiscoveryService;
    }

    @GetMapping("/api/workspaces/search")
    public ApiResponse<List<PlaceResponse>> search(
            @RequestParam("keyword") @NotBlank String keyword,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(value = "offset", defaultValue = "20") @Min(1) @Max(25) int offset
    ) {
        return ApiResponse.ok(placeDiscoveryService.search(keyword, city, page, offset));
    }

    @GetMapping("/api/workspaces/nearby")
    public ApiResponse<List<PlaceResponse>> nearby(
            @RequestParam("longitude") BigDecimal longitude,
            @RequestParam("latitude") BigDecimal latitude,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "radius", defaultValue = "1000") @Min(1) @Max(50000) int radius,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(value = "offset", defaultValue = "20") @Min(1) @Max(25) int offset
    ) {
        return ApiResponse.ok(placeDiscoveryService.nearby(longitude, latitude, keyword, radius, page, offset));
    }

    @GetMapping("/api/places/{placeId}")
    public ApiResponse<PlaceResponse> detail(@PathVariable long placeId) {
        return ApiResponse.ok(placeDiscoveryService.detail(placeId));
    }
}
