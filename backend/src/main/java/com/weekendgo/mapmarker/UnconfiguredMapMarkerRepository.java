package com.weekendgo.mapmarker;

import java.math.BigDecimal;
import java.util.List;

public class UnconfiguredMapMarkerRepository implements MapMarkerRepository {
    @Override
    public List<MapMarkerResponse> findNearbyMarkers(BigDecimal longitude, BigDecimal latitude, double radiusMeters, Long userId) {
        return List.of();
    }
}
