package com.weekendgo.checkin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckinRequest(
        @NotNull CrowdLevel crowdLevel,
        @NotNull NoiseLevel noiseLevel,
        @NotNull Boolean hasSeat,
        @Size(max = 500) String remark
) {
}
