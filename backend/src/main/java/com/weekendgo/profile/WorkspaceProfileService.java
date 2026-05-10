package com.weekendgo.profile;

import com.weekendgo.place.PlaceNotFoundException;
import com.weekendgo.place.PlaceRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceProfileService {

    private final PlaceRepository placeRepository;
    private final WorkspaceProfileRepository workspaceProfileRepository;

    public WorkspaceProfileService(
            PlaceRepository placeRepository,
            WorkspaceProfileRepository workspaceProfileRepository
    ) {
        this.placeRepository = placeRepository;
        this.workspaceProfileRepository = workspaceProfileRepository;
    }

    public ProfileSubmission submit(long placeId, long userId, ProfileSubmissionRequest request) {
        ensurePlaceExists(placeId);
        return workspaceProfileRepository.createSubmission(placeId, userId, request);
    }

    public ProfileSubmission approve(long submissionId, long adminId, String reason) {
        return workspaceProfileRepository.audit(submissionId, adminId, AuditStatus.APPROVED, reason);
    }

    public ProfileSubmission reject(long submissionId, long adminId, String reason) {
        return workspaceProfileRepository.audit(submissionId, adminId, AuditStatus.REJECTED, reason);
    }

    public WorkspaceProfile getPublicProfile(long placeId) {
        ensurePlaceExists(placeId);
        return workspaceProfileRepository.findProfileByPlaceId(placeId)
                .orElseThrow(WorkspaceProfileNotFoundException::new);
    }

    public List<MyProfileSubmissionResponse> mySubmissions(long userId) {
        return workspaceProfileRepository.findSubmissionsByUserId(userId).stream()
                .map(submission -> {
                    String placeName = placeRepository.findById(submission.placeId())
                            .map(com.weekendgo.place.Place::name)
                            .orElse("未知地点");
                    return new MyProfileSubmissionResponse(
                            submission.id(),
                            submission.placeId(),
                            placeName,
                            submission.userId(),
                            submission.quietScore(),
                            submission.wifiScore(),
                            submission.socketScore(),
                            submission.seatScore(),
                            submission.costScore(),
                            submission.minConsumption(),
                            submission.allowLongStay().name(),
                            submission.suitableScenes(),
                            submission.remark(),
                            submission.auditStatus(),
                            submission.createdAt()
                    );
                })
                .toList();
    }

    private void ensurePlaceExists(long placeId) {
        placeRepository.findById(placeId).orElseThrow(PlaceNotFoundException::new);
    }
}
