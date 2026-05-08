package com.weekendgo.amap.dto;

public record AmapPoi(
        String id,
        String name,
        String type,
        String address,
        String location,
        String district
) {
}
