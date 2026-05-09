package com.weekendgo.profile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProfileSubmissionRequest(
        @NotNull @DecimalMin("1.0") @DecimalMax("5.0") BigDecimal quietScore,
        @NotNull @DecimalMin("1.0") @DecimalMax("5.0") BigDecimal wifiScore,
        @NotNull @DecimalMin("1.0") @DecimalMax("5.0") BigDecimal socketScore,
        @NotNull @DecimalMin("1.0") @DecimalMax("5.0") BigDecimal seatScore,
        @DecimalMin("1.0") @DecimalMax("5.0") BigDecimal costScore,
        Integer minConsumption,
        AllowLongStay allowLongStay,
        List<@Size(max = 64) String> suitableScenes,
        @Size(max = 500) String remark
) {
    public AllowLongStay normalizedAllowLongStay() {
        return allowLongStay == null ? AllowLongStay.UNKNOWN : allowLongStay;
    }

    public List<String> normalizedSuitableScenes() {
        return suitableScenes == null ? List.of() : List.copyOf(suitableScenes);
    }
}
