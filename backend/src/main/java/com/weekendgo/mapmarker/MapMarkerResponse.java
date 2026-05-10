package com.weekendgo.mapmarker;

import java.math.BigDecimal;

public record MapMarkerResponse(
    long id,
    String name,
    BigDecimal longitude,
    BigDecimal latitude,
    String address,
    boolean marked,
    boolean favorited
) {}
