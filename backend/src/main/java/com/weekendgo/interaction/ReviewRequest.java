package com.weekendgo.interaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ReviewRequest(
        @NotNull @DecimalMin("1.0") @DecimalMax("5.0") BigDecimal quietScore,
        @NotNull @DecimalMin("1.0") @DecimalMax("5.0") BigDecimal wifiScore,
        @NotNull @DecimalMin("1.0") @DecimalMax("5.0") BigDecimal socketScore,
        @NotNull @DecimalMin("1.0") @DecimalMax("5.0") BigDecimal comfortScore,
        @NotNull @DecimalMin("1.0") @DecimalMax("5.0") BigDecimal costScore,
        @NotBlank @Size(max = 1000) String content,
        @Valid ProfileAttributeRequest profileAttributes,
        @Valid List<ReviewImageAttachment> images
) {
}
