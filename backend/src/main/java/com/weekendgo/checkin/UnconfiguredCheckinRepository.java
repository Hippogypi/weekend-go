package com.weekendgo.checkin;

import java.time.Instant;
import java.util.List;

public class UnconfiguredCheckinRepository implements CheckinRepository {

    @Override
    public SavedCheckin save(NewCheckin checkin) {
        throw new CheckinStorageException("Checkin storage is unavailable");
    }

    @Override
    public List<SavedCheckin> findRecentByPlaceId(long placeId, Instant cutoff) {
        return List.of();
    }
}
