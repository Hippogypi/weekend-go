package com.weekendgo.profile;

import java.math.BigDecimal;
import java.time.Instant;

public record WorkspaceProfile(
        long placeId,
        BigDecimal quietScore,
        BigDecimal wifiScore,
        BigDecimal socketScore,
        BigDecimal seatScore,
        BigDecimal costScore,
        Integer minConsumption,
        AllowLongStay allowLongStay,
        BigDecimal score,
        TrustLevel trustLevel,
        int approvedSubmissionCount,
        int contributorCount,
        Instant lastContributedAt
) {
}
