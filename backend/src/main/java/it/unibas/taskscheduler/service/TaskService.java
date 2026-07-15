package it.unibas.taskscheduler.service;

import it.unibas.taskscheduler.persistenza.IRepositoryTask;
import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import it.unibas.taskscheduler.rest.dto.TaskDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class TaskService {

    @Inject
    IRepositoryTask repositoryTask;

    @Inject
    IRepositoryWorkflow repositoryWorkflow;

    @Transactional
    public TaskDTO findById(Long id) {
        return repositoryTask.findByIdOptional(id)
                .map(task -> repositoryWorkflow.findByIdOptional(task.getWorkflowId())
                        .map(workflow -> {
                            workflow.ricostruisciFigli();
                            return workflow.getTasks().stream()
                                    .filter(t -> t.getId().equals(id))
                                    .findFirst()
                                    .orElse(task);
                        })
                        .orElse(task))
                .map(TaskDTO::from)
                .orElseThrow(() -> new NotFoundException("Task non trovato: " + id));
    }
}
