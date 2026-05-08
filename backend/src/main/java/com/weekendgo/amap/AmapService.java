package com.weekendgo.amap;

import com.weekendgo.amap.dto.AmapGeocode;
import com.weekendgo.amap.dto.AmapPoi;
import com.weekendgo.amap.dto.AmapReverseGeocode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AmapService {

    private final AmapClient client;

    public AmapService(AmapClient client) {
        this.client = client;
    }

    public List<AmapPoi> searchAround(String location, String keywords, int radius, int page, int offset) {
        return client.searchAround(location, keywords, radius, page, offset);
    }

    public List<AmapPoi> searchByKeyword(String keywords, String city, int page, int offset) {
        return client.searchByKeyword(keywords, city, page, offset);
    }

    public List<AmapGeocode> geocode(String address, String city) {
        return client.geocode(address, city);
    }

    public AmapReverseGeocode reverseGeocode(String location, int radius) {
        return client.reverseGeocode(location, radius);
    }
}
