package com.weekendgo.interaction;

import jakarta.validation.constraints.Size;
import java.util.List;

public record ProfileAttributeRequest(
        Integer minConsumption,
        @Size(max = 16) String allowLongStay,
        List<@Size(max = 64) String> suitableScenes
) {
}
