package com.weekendgo.interaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewImageAttachment(
        @NotBlank @Size(max = 512) String imageUrl,
        @Size(max = 500) String description
) {
}
