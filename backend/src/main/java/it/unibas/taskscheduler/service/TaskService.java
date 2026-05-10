package it.unibas.taskscheduler.service;

import it.unibas.taskscheduler.persistenza.IRepositoryTask;
import it.unibas.taskscheduler.rest.dto.TaskDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class TaskService {

    @Inject
    IRepositoryTask repositoryTask;

    public TaskDTO getTask(Long id) {
        return repositoryTask.findById(id)
                .map(TaskDTO::from)
                .orElseThrow(() -> new NotFoundException("Task non trovato: " + id));
    }
}
