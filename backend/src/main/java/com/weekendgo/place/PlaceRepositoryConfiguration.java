package com.weekendgo.place;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlaceRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(PlaceRepository.class)
    PlaceRepository unconfiguredPlaceRepository() {
        return new UnconfiguredPlaceRepository();
    }
}
