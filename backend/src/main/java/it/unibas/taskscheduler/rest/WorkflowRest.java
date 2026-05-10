package it.unibas.taskscheduler.rest;

import it.unibas.taskscheduler.rest.dto.GraphDTO;
import it.unibas.taskscheduler.rest.dto.WorkflowDTO;
import it.unibas.taskscheduler.rest.dto.WorkflowSummaryDTO;
import it.unibas.taskscheduler.service.WorkflowService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/workflow")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class WorkflowRest {

    @Inject
    WorkflowService workflowService;

    @GET
    public List<WorkflowSummaryDTO> listaWorkflow() {
        return workflowService.listaWorkflow();
    }

    @GET
    @Path("/{id}")
    public WorkflowDTO getWorkflow(@PathParam("id") Long id) {
        return workflowService.getWorkflow(id);
    }

    @GET
    @Path("/{id}/graph")
    public GraphDTO getGrafo(@PathParam("id") Long id) {
        return workflowService.getGrafo(id);
    }

    @POST
    @Path("/importa")
    public Response importa() {
        return Response.ok(workflowService.importaWorkflowDemo()).build();
    }

    @POST
    @Path("/{id}/avvia")
    public void avvia(@PathParam("id") Long id) {
        workflowService.avviaWorkflow(id);
    }

    @POST
    @Path("/{id}/pausa")
    public Response pausa(@PathParam("id") Long id) {
        try {
            workflowService.pausaWorkflow(id);
            return Response.noContent().build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/{id}/riprendi")
    public Response riprendi(@PathParam("id") Long id) {
        try {
            workflowService.riprendiWorkflow(id);
            return Response.noContent().build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/{id}/annulla")
    public Response annulla(@PathParam("id") Long id) {
        try {
            workflowService.annullaWorkflow(id);
            return Response.noContent().build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}
