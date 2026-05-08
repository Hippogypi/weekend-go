package com.weekendgo.amap.dto;

public record AmapGeocode(
        String formattedAddress,
        String province,
        String city,
        String district,
        String location
) {
}
