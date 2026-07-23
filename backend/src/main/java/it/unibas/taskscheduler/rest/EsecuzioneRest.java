package it.unibas.taskscheduler.rest;

import it.unibas.taskscheduler.rest.dto.EsecuzioneSummaryDTO;
import it.unibas.taskscheduler.rest.dto.EsecuzioneRuntimeDTO;
import it.unibas.taskscheduler.rest.dto.GraphDTO;
import it.unibas.taskscheduler.service.EsecuzioneService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/esecuzioni")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class EsecuzioneRest {

    @Inject
    EsecuzioneService esecuzioneService;

    @GET
    public List<EsecuzioneSummaryDTO> storia(@QueryParam("workflowId") Long workflowId) {
        return esecuzioneService.storia(workflowId);
    }

    @GET
    @Path("/{id}")
    public EsecuzioneSummaryDTO getEsecuzione(@PathParam("id") Long id) {
        return esecuzioneService.getEsecuzione(id);
    }

    @GET
    @Path("/{id}/graph")
    public GraphDTO getGrafo(@PathParam("id") Long id) {
        return esecuzioneService.getGrafo(id);
    }

    @GET
    @Path("/{id}/runtime")
    public EsecuzioneRuntimeDTO getRuntime(@PathParam("id") Long id) {
        return esecuzioneService.getRuntime(id);
    }

    @POST
    @Path("/{id}/pausa")
    public void pausa(@PathParam("id") Long id) {
        esecuzioneService.pausa(id);
    }

    @POST
    @Path("/{id}/riprendi")
    public void riprendi(@PathParam("id") Long id) {
        esecuzioneService.riprendi(id);
    }

    @POST
    @Path("/{id}/annulla")
    public void annulla(@PathParam("id") Long id) {
        esecuzioneService.annulla(id);
    }
}
