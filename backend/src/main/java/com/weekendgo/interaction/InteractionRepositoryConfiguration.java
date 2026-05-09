package com.weekendgo.interaction;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InteractionRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(InteractionRepository.class)
    InteractionRepository inMemoryInteractionRepository() {
        return new InMemoryInteractionRepository();
    }
}
