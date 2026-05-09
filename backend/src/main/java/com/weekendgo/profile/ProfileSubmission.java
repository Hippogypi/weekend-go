package com.weekendgo.profile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProfileSubmission(
        long id,
        long placeId,
        long userId,
        BigDecimal quietScore,
        BigDecimal wifiScore,
        BigDecimal socketScore,
        BigDecimal seatScore,
        BigDecimal costScore,
        Integer minConsumption,
        AllowLongStay allowLongStay,
        List<String> suitableScenes,
        String remark,
        AuditStatus auditStatus,
        Long auditedBy,
        Instant auditedAt,
        String auditReason,
        Instant createdAt
) {
}
