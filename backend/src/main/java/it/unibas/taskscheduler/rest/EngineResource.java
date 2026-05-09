package it.unibas.taskscheduler.rest;

import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import it.unibas.taskscheduler.rest.dto.EngineStatusDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/engine")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class EngineResource {

    @Inject
    IRepositoryWorkflow repositoryWorkflow;

    @GET
    @Path("/status")
    public EngineStatusDTO status() {
        return repositoryWorkflow.getWorkflowInCorso()
                .map(EngineStatusDTO::from)
                .orElse(EngineStatusDTO.inattivo());
    }
}
