package com.weekendgo.place;

import com.weekendgo.profile.WorkspaceProfile;
import java.math.BigDecimal;

public record PlaceResponse(
        long id,
        String amapPoiId,
        String name,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        String amapType,
        String amapTypeCode,
        String province,
        String city,
        String district,
        PlaceSource source,
        WorkspaceStatus workspaceStatus,
        WorkspaceProfile workspaceProfile,
        PlaceLinksResponse links
) {

    public static PlaceResponse from(Place place) {
        return from(place, null);
    }

    public static PlaceResponse from(Place place, WorkspaceProfile workspaceProfile) {
        return new PlaceResponse(
                place.id(),
                place.amapPoiId(),
                place.name(),
                place.address(),
                place.longitude(),
                place.latitude(),
                place.amapType(),
                place.amapTypeCode(),
                place.province(),
                place.city(),
                place.district(),
                place.source(),
                place.workspaceStatus(),
                workspaceProfile,
                PlaceLinksResponse.forPlace(place.id())
        );
    }
}
