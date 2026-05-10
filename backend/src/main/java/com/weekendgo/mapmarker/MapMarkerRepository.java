package com.weekendgo.mapmarker;

import java.math.BigDecimal;
import java.util.List;

public interface MapMarkerRepository {
    List<MapMarkerResponse> findNearbyMarkers(BigDecimal longitude, BigDecimal latitude, double radiusMeters, Long userId);
}
