package com.weekendgo.profile;

import com.weekendgo.interaction.PendingAuditItem;
import java.util.List;
import java.util.Optional;

public interface WorkspaceProfileRepository {

    ProfileSubmission createSubmission(long placeId, long userId, ProfileSubmissionRequest request);

    ProfileSubmission audit(long submissionId, long adminId, AuditStatus status, String reason);

    Optional<ProfileSubmission> findSubmissionById(long submissionId);

    Optional<WorkspaceProfile> findProfileByPlaceId(long placeId);

    List<ProfileSubmission> findSubmissionsByUserId(long userId);

    List<PendingAuditItem> findPendingProfileSubmissions(int page, int size);

    long countPendingProfileSubmissions();
}
