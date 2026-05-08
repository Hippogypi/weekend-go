package com.weekendgo.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class UserAccountRepositoryConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    JdbcTemplateAutoConfiguration.class,
                    TransactionAutoConfiguration.class,
                    DataSourceTransactionManagerAutoConfiguration.class
            ))
            .withUserConfiguration(
                    UserAccountRepositoryConfiguration.class,
                    JdbcUserAccountRepository.class
            );

    @Test
    void usesInMemoryRepositoryWhenDatasourceUrlIsMissing() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(UserAccountRepository.class)
                .getBean(UserAccountRepository.class)
                .isInstanceOf(InMemoryUserAccountRepository.class));
    }

    @Test
    void usesJdbcRepositoryWhenDatasourceUrlIsConfigured() {
        contextRunner
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:auth-config;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                        "spring.datasource.driver-class-name=org.h2.Driver"
                )
                .run(context -> assertThat(context)
                        .hasSingleBean(UserAccountRepository.class)
                        .getBean(UserAccountRepository.class)
                        .isInstanceOf(JdbcUserAccountRepository.class));
    }
}
