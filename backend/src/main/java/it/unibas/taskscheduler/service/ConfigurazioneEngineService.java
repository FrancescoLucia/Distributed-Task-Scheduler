package it.unibas.taskscheduler.service;

import it.unibas.taskscheduler.modello.ConfigurazioneEngine;
import it.unibas.taskscheduler.modello.RetryPolicy;
import it.unibas.taskscheduler.persistenza.IRepositoryConfigurazioneEngine;
import it.unibas.taskscheduler.rest.dto.ConfigurazioneEngineDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConfigurazioneEngineService {

    private static final ConfigurazioneEngine DEFAULT_CONFIG =
            new ConfigurazioneEngine(new RetryPolicy(0, 0));

    @Inject
    IRepositoryConfigurazioneEngine repositoryConfigurazione;

    public ConfigurazioneEngineDTO getConfigurazione() {
        return ConfigurazioneEngineDTO.from(
                repositoryConfigurazione.find().orElse(DEFAULT_CONFIG));
    }

    public void aggiorna(ConfigurazioneEngineDTO dto) {
        repositoryConfigurazione.persist(dto.toModel());
    }
}
