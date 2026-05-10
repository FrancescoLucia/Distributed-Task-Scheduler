package it.unibas.taskscheduler.rest;

import it.unibas.taskscheduler.rest.dto.EngineStatusDTO;
import it.unibas.taskscheduler.service.EngineService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/engine")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class EngineRest {

    @Inject
    EngineService engineService;

    @GET
    @Path("/status")
    public EngineStatusDTO status() {
        return engineService.getStatus();
    }
}
