package com.weekendgo.place;

import com.weekendgo.amap.dto.AmapPoi;
import java.util.List;
import java.util.Optional;

public class UnconfiguredPlaceRepository implements PlaceRepository {

    @Override
    public List<Place> saveAllFromAmap(List<AmapPoi> pois) {
        throw new PlaceStorageException("spring.datasource.url is required for place persistence");
    }

    @Override
    public Optional<Place> findById(long id) {
        throw new PlaceStorageException("spring.datasource.url is required for place persistence");
    }
}
