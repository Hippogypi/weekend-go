package com.weekendgo.profile;

import jakarta.validation.constraints.Size;

public record AuditRequest(@Size(max = 500) String reason) {
}
