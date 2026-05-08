package com.weekendgo.amap.dto;

public record AmapReverseGeocode(
        String formattedAddress,
        String province,
        String city,
        String district
) {
}
