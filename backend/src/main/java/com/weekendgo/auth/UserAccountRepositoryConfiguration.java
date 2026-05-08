package com.weekendgo.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserAccountRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(UserAccountRepository.class)
    UserAccountRepository inMemoryUserAccountRepository() {
        return new InMemoryUserAccountRepository();
    }
}
