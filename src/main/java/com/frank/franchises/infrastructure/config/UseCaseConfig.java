package com.frank.franchises.infrastructure.config;

import com.frank.franchises.application.usecase.FranchiseUseCaseImpl;
import com.frank.franchises.domain.port.in.FranchiseUseCase;
import com.frank.franchises.domain.port.out.FranchiseRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public FranchiseUseCase franchiseUseCase(FranchiseRepositoryPort repositoryPort) {
        return new FranchiseUseCaseImpl(repositoryPort);
    }
}
