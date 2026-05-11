package com.weekendgo.qa;

import static org.assertj.core.api.Assertions.assertThat;

import com.weekendgo.common.data.DataAccessConfiguration;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

class QaRepositoryConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JdbcTemplateAutoConfiguration.class,
                    DataSourceTransactionManagerAutoConfiguration.class,
                    TransactionAutoConfiguration.class
            ))
            .withUserConfiguration(
                    DataAccessConfiguration.class,
                    JdbcQaRepository.class,
                    QaRepositoryConfiguration.class
            );

    @Test
    void defaultContextUsesUnconfiguredRepositoryWithoutCreatingDataSource() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(DataSource.class);
            assertThat(context).doesNotHaveBean(JdbcTemplate.class);
            assertThat(context).hasSingleBean(QaRepository.class);
            assertThat(context.getBean(QaRepository.class)).isInstanceOf(UnconfiguredQaRepository.class);
        });
    }

    @Test
    void datasourceUrlEnablesSpringJdbcQaRepository() {
        contextRunner
                .withPropertyValues("spring.datasource.url=jdbc:h2:mem:qa-config;MODE=MySQL;DATABASE_TO_LOWER=TRUE")
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSource.class);
                    assertThat(context).hasSingleBean(JdbcTemplate.class);
                    assertThat(context).hasSingleBean(QaRepository.class);
                    assertThat(context.getBean(QaRepository.class)).isInstanceOf(JdbcQaRepository.class);
                });
    }
}
