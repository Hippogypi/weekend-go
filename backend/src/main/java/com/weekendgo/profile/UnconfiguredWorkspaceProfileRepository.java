package com.weekendgo.profile;

import java.util.Optional;

public class UnconfiguredWorkspaceProfileRepository implements WorkspaceProfileRepository {

    @Override
    public Optional<WorkspaceProfile> findProfileByPlaceId(long placeId) {
        return Optional.empty();
    }
}
