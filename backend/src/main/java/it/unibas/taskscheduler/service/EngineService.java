package it.unibas.taskscheduler.service;

import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import it.unibas.taskscheduler.rest.dto.EngineStatusDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class EngineService {

    @Inject
    IRepositoryWorkflow repositoryWorkflow;

    @Transactional
    public EngineStatusDTO getStatus() {
        return repositoryWorkflow.getWorkflowInCorso()
                .map(EngineStatusDTO::from)
                .orElse(EngineStatusDTO.inattivo());
    }
}
