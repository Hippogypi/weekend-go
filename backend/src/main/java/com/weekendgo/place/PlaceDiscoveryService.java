package com.weekendgo.place;

import com.weekendgo.amap.AmapService;
import com.weekendgo.amap.dto.AmapPoi;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PlaceDiscoveryService {

    private final AmapService amapService;
    private final PlaceRepository placeRepository;

    public PlaceDiscoveryService(AmapService amapService, PlaceRepository placeRepository) {
        this.amapService = amapService;
        this.placeRepository = placeRepository;
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
                .map(PlaceResponse::from)
                .orElseThrow(PlaceNotFoundException::new);
    }

    private List<PlaceResponse> saveAndMap(List<AmapPoi> pois) {
        return placeRepository.saveAllFromAmap(pois).stream()
                .map(PlaceResponse::from)
                .toList();
    }
}
