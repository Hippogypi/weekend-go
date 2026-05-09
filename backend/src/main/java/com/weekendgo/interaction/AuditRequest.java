package com.weekendgo.interaction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AuditRequest(
        @NotNull AuditStatus auditStatus,
        @Size(max = 500) String reason
) {
}
