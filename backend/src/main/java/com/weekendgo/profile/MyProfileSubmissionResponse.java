package com.weekendgo.profile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MyProfileSubmissionResponse(
        long id,
        long placeId,
        String placeName,
        long userId,
        BigDecimal quietScore,
        BigDecimal wifiScore,
        BigDecimal socketScore,
        BigDecimal seatScore,
        BigDecimal costScore,
        Integer minConsumption,
        String allowLongStay,
        List<String> suitableScenes,
        String remark,
        AuditStatus auditStatus,
        Instant createdAt
) {
}
