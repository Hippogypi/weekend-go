package com.weekendgo.interaction;

public record AuditStats(
        long pendingProfiles,
        long pendingReviews,
        long pendingImages,
        long todayApproved,
        long todayRejected
) {
}
