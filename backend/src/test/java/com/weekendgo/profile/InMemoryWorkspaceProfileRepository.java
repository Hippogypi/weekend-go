package com.weekendgo.profile;

import com.weekendgo.interaction.PendingAuditItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryWorkspaceProfileRepository implements WorkspaceProfileRepository {

    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<Long, ProfileSubmission> submissions = new LinkedHashMap<>();
    private final Map<Long, WorkspaceProfile> profiles = new LinkedHashMap<>();

    @Override
    public ProfileSubmission createSubmission(long placeId, long userId, ProfileSubmissionRequest request) {
        long id = sequence.getAndIncrement();
        ProfileSubmission submission = new ProfileSubmission(
                id,
                placeId,
                userId,
                request.quietScore(),
                request.wifiScore(),
                request.socketScore(),
                request.seatScore(),
                request.costScore(),
                request.minConsumption(),
                request.normalizedAllowLongStay(),
                request.normalizedSuitableScenes(),
                request.remark(),
                AuditStatus.PENDING,
                null,
                null,
                null,
                Instant.now()
        );
        submissions.put(id, submission);
        return submission;
    }

    @Override
    public ProfileSubmission audit(long submissionId, long adminId, AuditStatus status, String reason) {
        ProfileSubmission current = findSubmissionById(submissionId).orElseThrow(ProfileSubmissionNotFoundException::new);
        ProfileSubmission audited = new ProfileSubmission(
                current.id(),
                current.placeId(),
                current.userId(),
                current.quietScore(),
                current.wifiScore(),
                current.socketScore(),
                current.seatScore(),
                current.costScore(),
                current.minConsumption(),
                current.allowLongStay(),
                current.suitableScenes(),
                current.remark(),
                status,
                adminId,
                Instant.now(),
                reason,
                current.createdAt()
        );
        submissions.put(submissionId, audited);
        rebuildProfile(current.placeId());
        return audited;
    }

    @Override
    public Optional<ProfileSubmission> findSubmissionById(long submissionId) {
        return Optional.ofNullable(submissions.get(submissionId));
    }

    @Override
    public Optional<WorkspaceProfile> findProfileByPlaceId(long placeId) {
        return Optional.ofNullable(profiles.get(placeId));
    }

    @Override
    public List<ProfileSubmission> findSubmissionsByUserId(long userId) {
        return submissions.values().stream()
                .filter(submission -> submission.userId() == userId)
                .sorted(java.util.Comparator.comparing(ProfileSubmission::createdAt).reversed())
                .toList();
    }

    @Override
    public List<PendingAuditItem> findPendingProfileSubmissions(int page, int size) {
        return submissions.values().stream()
                .filter(submission -> submission.auditStatus() == AuditStatus.PENDING)
                .skip((long) (page - 1) * size)
                .limit(size)
                .map(submission -> new PendingAuditItem(
                        submission.id(),
                        submission.placeId(),
                        "Unknown",
                        submission.userId(),
                        "Unknown",
                        submission.remark(),
                        submission.createdAt(),
                        "profile"
                ))
                .toList();
    }

    @Override
    public long countPendingProfileSubmissions() {
        return submissions.values().stream()
                .filter(submission -> submission.auditStatus() == AuditStatus.PENDING)
                .count();
    }

    private void rebuildProfile(long placeId) {
        List<ProfileSubmission> approved = submissions.values().stream()
                .filter(submission -> submission.placeId() == placeId)
                .filter(submission -> submission.auditStatus() == AuditStatus.APPROVED)
                .toList();
        if (approved.isEmpty()) {
            profiles.remove(placeId);
            return;
        }
        WorkspaceProfile profile = new WorkspaceProfile(
                placeId,
                average(approved.stream().map(ProfileSubmission::quietScore).toList(), 1),
                average(approved.stream().map(ProfileSubmission::wifiScore).toList(), 1),
                average(approved.stream().map(ProfileSubmission::socketScore).toList(), 1),
                average(approved.stream().map(ProfileSubmission::seatScore).toList(), 1),
                average(approved.stream().map(ProfileSubmission::costScore).toList(), 1),
                approved.stream().map(ProfileSubmission::minConsumption).filter(value -> value != null).min(Integer::compareTo).orElse(null),
                approved.get(0).allowLongStay(),
                average(List.of(average(approved.stream().map(ProfileSubmission::quietScore).toList(), 2),
                        average(approved.stream().map(ProfileSubmission::wifiScore).toList(), 2),
                        average(approved.stream().map(ProfileSubmission::socketScore).toList(), 2),
                        average(approved.stream().map(ProfileSubmission::seatScore).toList(), 2),
                        average(approved.stream().map(ProfileSubmission::costScore).toList(), 2)), 2),
                TrustLevel.LOW,
                approved.size(),
                (int) approved.stream().map(ProfileSubmission::userId).distinct().count(),
                approved.stream().map(ProfileSubmission::createdAt).max(Instant::compareTo).orElse(null)
        );
        profiles.put(placeId, profile);
    }

    private BigDecimal average(List<BigDecimal> values, int scale) {
        List<BigDecimal> present = new ArrayList<>();
        for (BigDecimal value : values) {
            if (value != null) {
                present.add(value);
            }
        }
        if (present.isEmpty()) {
            return null;
        }
        BigDecimal sum = present.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(present.size()), scale, RoundingMode.HALF_UP);
    }
}
