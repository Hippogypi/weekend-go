package com.weekendgo.place;

import com.weekendgo.amap.AmapService;
import com.weekendgo.amap.dto.AmapPoi;
import com.weekendgo.profile.WorkspaceProfileRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PlaceDiscoveryService {

    private final AmapService amapService;
    private final PlaceRepository placeRepository;
    private final WorkspaceProfileRepository workspaceProfileRepository;

    public PlaceDiscoveryService(
            AmapService amapService,
            PlaceRepository placeRepository,
            WorkspaceProfileRepository workspaceProfileRepository
    ) {
        this.amapService = amapService;
        this.placeRepository = placeRepository;
        this.workspaceProfileRepository = workspaceProfileRepository;
    }

    public List<PlaceResponse> search(String keyword, String city, int page, int offset) {
        return saveAndMap(amapService.searchByKeyword(keyword, city, page, offset));
    }

    public List<PlaceResponse> nearby(
            BigDecimal longitude,
            BigDecimal latitude,
            String keyword,
            int radius,
            int page,
            int offset
    ) {
        String location = longitude.toPlainString() + "," + latitude.toPlainString();
        return saveAndMap(amapService.searchAround(location, keyword, radius, page, offset));
    }

    public PlaceResponse detail(long placeId) {
        return placeRepository.findById(placeId)
                .map(place -> PlaceResponse.from(place, workspaceProfileRepository.findProfileByPlaceId(place.id()).orElse(null)))
                .orElseThrow(PlaceNotFoundException::new);
    }

    private List<PlaceResponse> saveAndMap(List<AmapPoi> pois) {
        return placeRepository.saveAllFromAmap(pois).stream()
                .map(place -> PlaceResponse.from(place, workspaceProfileRepository.findProfileByPlaceId(place.id()).orElse(null)))
                .toList();
    }
}
