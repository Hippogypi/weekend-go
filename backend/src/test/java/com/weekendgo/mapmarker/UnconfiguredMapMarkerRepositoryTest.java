package com.weekendgo.mapmarker;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UnconfiguredMapMarkerRepositoryTest {

    @Test
    void returnsEmptyListWhenNoDatabaseConfigured() {
        UnconfiguredMapMarkerRepository repository = new UnconfiguredMapMarkerRepository();
        assertThat(repository.findNearbyMarkers(new BigDecimal("116.4"), new BigDecimal("39.9"), 5000, null)).isEmpty();
    }
}
