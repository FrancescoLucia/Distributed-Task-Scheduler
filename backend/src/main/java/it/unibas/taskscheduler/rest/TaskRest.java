package it.unibas.taskscheduler.rest;

import it.unibas.taskscheduler.rest.dto.TaskDTO;
import it.unibas.taskscheduler.service.TaskService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/tasks")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class TaskRest {

    @Inject
    TaskService taskService;

    @GET
    @Path("/{id}")
    public TaskDTO getTask(@PathParam("id") Long id) {
        return taskService.findById(id);
    }
}
