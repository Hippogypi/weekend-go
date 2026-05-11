package com.weekendgo.profile;

import java.util.Optional;

public interface WorkspaceProfileRepository {

    Optional<WorkspaceProfile> findProfileByPlaceId(long placeId);
}
