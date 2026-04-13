package com.jcode.jclinical.infrastructure.configuration;

import com.jcode.jclinical.core.application.ExpedienteService;
import com.jcode.jclinical.core.application.PlantillaService;
import com.jcode.jclinical.core.port.in.GestionarExpedienteUseCase;
import com.jcode.jclinical.core.port.in.GestionarPlantillaUseCase;
import com.jcode.jclinical.core.port.out.ExpedienteRepositoryPort;
import com.jcode.jclinical.core.port.out.PlantillaRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreConfig {

    @Bean
    public GestionarExpedienteUseCase gestionarExpedienteUseCase(ExpedienteRepositoryPort repositoryPort) {
        return new ExpedienteService(repositoryPort);
    }

    @Bean
    public GestionarPlantillaUseCase gestionarPlantillaUseCase(PlantillaRepositoryPort repositoryPort) {
        return new PlantillaService(repositoryPort);
    }
}