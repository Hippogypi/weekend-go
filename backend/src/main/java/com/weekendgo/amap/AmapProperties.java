package com.weekendgo.amap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weekend-go.amap")
public record AmapProperties(
        String apiKey,
        String baseUrl
) {

    public AmapProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://restapi.amap.com";
        }
    }
}
