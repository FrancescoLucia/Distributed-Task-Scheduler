package it.unibas.taskscheduler.rest;

import it.unibas.taskscheduler.persistenza.IRepositoryTask;
import it.unibas.taskscheduler.rest.dto.TaskDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/tasks")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class TaskResource {

    @Inject
    IRepositoryTask repositoryTask;

    @GET
    @Path("/{id}")
    public TaskDTO getTask(@PathParam("id") Long id) {
        return repositoryTask.findById(id)
                .map(TaskDTO::from)
                .orElseThrow(() -> new NotFoundException("Task non trovato: " + id));
    }
}
