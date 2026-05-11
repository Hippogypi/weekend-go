package com.weekendgo.profile;

import com.weekendgo.place.PlaceNotFoundException;
import com.weekendgo.place.PlaceRepository;
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

    public WorkspaceProfile getPublicProfile(long placeId) {
        ensurePlaceExists(placeId);
        return workspaceProfileRepository.findProfileByPlaceId(placeId)
                .orElseThrow(WorkspaceProfileNotFoundException::new);
    }

    private void ensurePlaceExists(long placeId) {
        placeRepository.findById(placeId).orElseThrow(PlaceNotFoundException::new);
    }
}
