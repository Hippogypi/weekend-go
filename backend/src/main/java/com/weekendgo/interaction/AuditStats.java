package com.weekendgo.interaction;

public record AuditStats(
        long pendingReviews,
        long pendingImages,
        long todayApproved,
        long todayRejected
) {
}
