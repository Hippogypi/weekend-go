package com.weekendgo.place;

import com.weekendgo.amap.dto.AmapPoi;
import java.util.List;
import java.util.Optional;

public interface PlaceRepository {

    List<Place> saveAllFromAmap(List<AmapPoi> pois);

    Optional<Place> findById(long id);
}
