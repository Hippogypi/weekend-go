package com.weekendgo.qa;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QaRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(QaRepository.class)
    QaRepository unconfiguredQaRepository() {
        return new UnconfiguredQaRepository();
    }
}
