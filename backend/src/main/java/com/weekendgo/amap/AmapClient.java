package com.weekendgo.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.weekendgo.amap.dto.AmapGeocode;
import com.weekendgo.amap.dto.AmapPoi;
import com.weekendgo.amap.dto.AmapReverseGeocode;
import com.weekendgo.amap.exception.AmapServiceException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class AmapClient {

    private final RestTemplate restTemplate;
    private final AmapProperties properties;

    public AmapClient(RestTemplate restTemplate, AmapProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public List<AmapPoi> searchAround(String location, String keywords, int radius, int page, int offset) {
        URI uri = baseUri("/v3/place/around")
                .queryParam("location", location)
                .queryParamIfPresent("keywords", optionalParam(keywords))
                .queryParam("radius", radius)
                .queryParam("offset", offset)
                .queryParam("page", page)
                .queryParam("extensions", "base")
                .build()
                .encode()
                .toUri();

        return parsePois(exchange(uri));
    }

    public List<AmapPoi> searchByKeyword(String keywords, String city, int page, int offset) {
        URI uri = baseUri("/v3/place/text")
                .queryParam("keywords", keywords)
                .queryParamIfPresent("city", optionalParam(city))
                .queryParam("offset", offset)
                .queryParam("page", page)
                .queryParam("extensions", "base")
                .build()
                .encode()
                .toUri();

        return parsePois(exchange(uri));
    }

    public List<AmapGeocode> geocode(String address, String city) {
        URI uri = baseUri("/v3/geocode/geo")
                .queryParam("address", address)
                .queryParamIfPresent("city", optionalParam(city))
                .build()
                .encode()
                .toUri();

        JsonNode response = exchange(uri);
        List<AmapGeocode> geocodes = new ArrayList<>();
        for (JsonNode item : response.path("geocodes")) {
            geocodes.add(new AmapGeocode(
                    text(item, "formatted_address"),
                    text(item, "province"),
                    text(item, "city"),
                    text(item, "district"),
                    text(item, "location")
            ));
        }
        return geocodes;
    }

    public AmapReverseGeocode reverseGeocode(String location, int radius) {
        URI uri = baseUri("/v3/geocode/regeo")
                .queryParam("location", location)
                .queryParam("radius", radius)
                .queryParam("extensions", "base")
                .build()
                .encode()
                .toUri();

        JsonNode regeocode = exchange(uri).path("regeocode");
        JsonNode component = regeocode.path("addressComponent");
        return new AmapReverseGeocode(
                text(regeocode, "formatted_address"),
                text(component, "province"),
                text(component, "city"),
                text(component, "district")
        );
    }

    private JsonNode exchange(URI uri) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new AmapServiceException("Amap api key is not configured");
        }
        try {
            JsonNode response = restTemplate.getForObject(uri, JsonNode.class);
            if (response == null) {
                throw new AmapServiceException("Amap request failed: empty response");
            }
            if (!"1".equals(response.path("status").asText())) {
                String info = response.path("info").asText("UNKNOWN_ERROR");
                String infoCode = response.path("infocode").asText("");
                throw new AmapServiceException("Amap request failed: " + info + formatInfoCode(infoCode));
            }
            return response;
        } catch (RestClientException exception) {
            throw new AmapServiceException("Amap request failed", exception);
        }
    }

    private UriComponentsBuilder baseUri(String path) {
        return UriComponentsBuilder
                .fromHttpUrl(properties.baseUrl())
                .path(path)
                .queryParam("key", properties.apiKey());
    }

    private List<AmapPoi> parsePois(JsonNode response) {
        List<AmapPoi> pois = new ArrayList<>();
        for (JsonNode item : response.path("pois")) {
            pois.add(new AmapPoi(
                    text(item, "id"),
                    text(item, "name"),
                    text(item, "type"),
                    text(item, "address"),
                    text(item, "location"),
                    text(item, "adname")
            ));
        }
        return pois;
    }

    private static java.util.Optional<String> optionalParam(String value) {
        return StringUtils.hasText(value) ? java.util.Optional.of(value) : java.util.Optional.empty();
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static String formatInfoCode(String infoCode) {
        return StringUtils.hasText(infoCode) ? " (" + infoCode + ")" : "";
    }
}
