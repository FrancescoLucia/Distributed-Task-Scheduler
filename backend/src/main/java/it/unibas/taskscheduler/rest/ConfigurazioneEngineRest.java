package it.unibas.taskscheduler.rest;

import it.unibas.taskscheduler.rest.dto.ConfigurazioneEngineDTO;
import it.unibas.taskscheduler.service.ConfigurazioneEngineService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/engine/configurazione")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigurazioneEngineRest {

    @Inject
    ConfigurazioneEngineService configurazioneEngineService;

    @GET
    public ConfigurazioneEngineDTO get() {
        return configurazioneEngineService.getConfigurazione();
    }

    @PUT
    public Response aggiorna(ConfigurazioneEngineDTO dto) {
        configurazioneEngineService.aggiorna(dto);
        return Response.noContent().build();
    }
}
