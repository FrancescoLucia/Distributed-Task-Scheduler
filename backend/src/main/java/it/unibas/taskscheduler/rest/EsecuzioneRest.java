package it.unibas.taskscheduler.rest;

import it.unibas.taskscheduler.rest.dto.EsecuzioneSummaryDTO;
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
import jakarta.ws.rs.core.Response;

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

    @POST
    @Path("/{id}/pausa")
    public Response pausa(@PathParam("id") Long id) {
        try {
            esecuzioneService.pausa(id);
            return Response.noContent().build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/{id}/riprendi")
    public Response riprendi(@PathParam("id") Long id) {
        try {
            esecuzioneService.riprendi(id);
            return Response.noContent().build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/{id}/annulla")
    public Response annulla(@PathParam("id") Long id) {
        try {
            esecuzioneService.annulla(id);
            return Response.noContent().build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}
