package it.unibas.taskscheduler.rest;

import it.unibas.taskscheduler.rest.dto.GraphDTO;
import it.unibas.taskscheduler.rest.dto.RinominaWorkflowDTO;
import it.unibas.taskscheduler.rest.dto.WorkflowCatalogoDTO;
import it.unibas.taskscheduler.service.EsecuzioneService;
import it.unibas.taskscheduler.service.WorkflowService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/workflow")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class WorkflowRest {

    @Inject
    WorkflowService workflowService;

    @Inject
    EsecuzioneService esecuzioneService;

    @GET
    public List<WorkflowCatalogoDTO> listaWorkflow(@QueryParam("nome") String nome) {
        return workflowService.listaWorkflow(nome);
    }

    @GET
    @Path("/{id}/graph")
    public GraphDTO getGrafo(@PathParam("id") Long id) {
        return workflowService.getGrafo(id);
    }

    @POST
    @Path("/importa")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importa(String corpo) {
        return Response.ok(Map.of("id", workflowService.importaWorkflow(corpo))).build();
    }

    @POST
    @Path("/importa/demo")
    public Response importaDemo() {
        return Response.ok(Map.of("id", workflowService.importaWorkflowDemo())).build();
    }

    @GET
    @Path("/template")
    public Response template() {
        return Response.ok(workflowService.templateWorkflow())
                .header("Content-Disposition", "attachment; filename=\"workflow-template.json\"")
                .build();
    }

    @PUT
    @Path("/{id}/nome")
    @Consumes(MediaType.APPLICATION_JSON)
    public void rinomina(@PathParam("id") Long id, RinominaWorkflowDTO dto) {
        workflowService.rinominaWorkflow(id, dto != null ? dto.getNome() : null);
        
    }

    @DELETE
    @Path("/{id}")
    public void elimina(@PathParam("id") Long id) {
        workflowService.eliminaWorkflow(id);
    }

    @POST
    @Path("/{id}/avvia")
    public Response avvia(@PathParam("id") Long id) {
        return Response.ok(Map.of("esecuzioneId", esecuzioneService.avvia(id))).build();
    }
}
