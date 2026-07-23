package it.unibas.taskscheduler.service;

import it.unibas.taskscheduler.persistenza.IRepositoryEsecuzione;
import it.unibas.taskscheduler.rest.dto.EngineStatusDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class EngineService {

    @Inject
    IRepositoryEsecuzione repositoryEsecuzione;

    @Transactional
    public EngineStatusDTO getStatus() {
        return repositoryEsecuzione.getEsecuzioneInCorso()
                .map(EngineStatusDTO::from)
                .orElse(EngineStatusDTO.inattivo());
    }
}
