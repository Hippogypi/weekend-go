package com.weekendgo.checkin;

import java.time.Instant;
import java.util.List;

public interface CheckinRepository {

    SavedCheckin save(NewCheckin checkin);

    List<SavedCheckin> findRecentByPlaceId(long placeId, Instant cutoff);
}
