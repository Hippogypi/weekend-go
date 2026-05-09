package com.weekendgo.checkin;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CheckinRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(CheckinRepository.class)
    CheckinRepository unconfiguredCheckinRepository() {
        return new UnconfiguredCheckinRepository();
    }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock clock() {
        return Clock.systemUTC();
    }
}
