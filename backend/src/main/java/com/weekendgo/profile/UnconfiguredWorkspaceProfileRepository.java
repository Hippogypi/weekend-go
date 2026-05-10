package com.weekendgo.profile;

import com.weekendgo.interaction.PendingAuditItem;
import java.util.List;
import java.util.Optional;

public class UnconfiguredWorkspaceProfileRepository implements WorkspaceProfileRepository {

    @Override
    public ProfileSubmission createSubmission(long placeId, long userId, ProfileSubmissionRequest request) {
        throw new ProfileStorageException("spring.datasource.url is required for workspace profile persistence");
    }

    @Override
    public ProfileSubmission audit(long submissionId, long adminId, AuditStatus status, String reason) {
        throw new ProfileStorageException("spring.datasource.url is required for workspace profile persistence");
    }

    @Override
    public Optional<ProfileSubmission> findSubmissionById(long submissionId) {
        throw new ProfileStorageException("spring.datasource.url is required for workspace profile persistence");
    }

    @Override
    public Optional<WorkspaceProfile> findProfileByPlaceId(long placeId) {
        return Optional.empty();
    }

    @Override
    public List<PendingAuditItem> findPendingProfileSubmissions(int page, int size) {
        throw new ProfileStorageException("spring.datasource.url is required for workspace profile persistence");
    }

    @Override
    public long countPendingProfileSubmissions() {
        throw new ProfileStorageException("spring.datasource.url is required for workspace profile persistence");
    }
}
