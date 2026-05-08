package com.weekendgo.place;

import java.math.BigDecimal;

public record Place(
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
        WorkspaceStatus workspaceStatus
) {
}
