package com.weekendgo.profile;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkspaceProfileRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(WorkspaceProfileRepository.class)
    WorkspaceProfileRepository unconfiguredWorkspaceProfileRepository() {
        return new UnconfiguredWorkspaceProfileRepository();
    }
}
