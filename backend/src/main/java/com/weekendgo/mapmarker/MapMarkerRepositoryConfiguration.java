package com.weekendgo.mapmarker;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapMarkerRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(MapMarkerRepository.class)
    MapMarkerRepository unconfiguredMapMarkerRepository() {
        return new UnconfiguredMapMarkerRepository();
    }
}
