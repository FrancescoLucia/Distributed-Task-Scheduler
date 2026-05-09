package it.unibas.taskscheduler.rest;

import it.unibas.taskscheduler.command.ScriptCommand;
import it.unibas.taskscheduler.engine.Engine;
import it.unibas.taskscheduler.modello.Task;
import it.unibas.taskscheduler.modello.Workflow;
import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import it.unibas.taskscheduler.rest.dto.GraphDTO;
import it.unibas.taskscheduler.rest.dto.WorkflowDTO;
import it.unibas.taskscheduler.rest.dto.WorkflowSummaryDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Path("/workflow")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class WorkflowResource {

    @Inject
    Engine engine;

    @Inject
    IRepositoryWorkflow repositoryWorkflow;

    @GET
    public List<WorkflowSummaryDTO> listaWorkflow() {
        return repositoryWorkflow.findAll().stream()
                .map(WorkflowSummaryDTO::from)
                .toList();
    }

    @GET
    @Path("/{id}")
    public WorkflowDTO getWorkflow(@PathParam("id") Long id) {
        return repositoryWorkflow.findById(id)
                .map(WorkflowDTO::from)
                .orElseThrow(() -> new NotFoundException("Workflow non trovato: " + id));
    }

    @GET
    @Path("/{id}/graph")
    public GraphDTO getGrafo(@PathParam("id") Long id) {
        return repositoryWorkflow.findById(id)
                .map(GraphDTO::from)
                .orElseThrow(() -> new NotFoundException("Workflow non trovato: " + id));
    }

    @POST
    @Path("/importa")
    public Response importa() {
        Task taskA = new Task("Task-A", new ScriptCommand("sleep 1 && echo 'Test 1 done'"));
        Task taskB = new Task("Task-B", new ScriptCommand("sleep 1 && echo 'Test 2 done'"));
        Task taskC = new Task("Task-C", new ScriptCommand("sleep 1 && echo 'Test 3 done'"));
        Task taskD = new Task("Task-D", new ScriptCommand("sleep 1 && echo 'Test 4 done'"));
        Task taskE = new Task("Task-E", new ScriptCommand("sleep 1 && echo 'Test 5 done'"));

        taskB.getDipendenze().add(taskA);
        taskC.getDipendenze().add(taskA);
        taskE.getDipendenze().add(taskA);
        taskD.getDipendenze().add(taskB);
        taskD.getDipendenze().add(taskC);

        Workflow workflow = new Workflow();
        workflow.setNome("Workflow Demo");
        workflow.aggiungiTask(taskA);
        workflow.aggiungiTask(taskB);
        workflow.aggiungiTask(taskC);
        workflow.aggiungiTask(taskD);
        workflow.aggiungiTask(taskE);

        engine.importaWorkflow(workflow);

        return Response.ok(workflow.getId()).build();
    }

    @POST
    @Path("/{id}/avvia")
    public Response avvia(@PathParam("id") Long id) {
        repositoryWorkflow.getWorkflowInCorso().ifPresent(workflowInCorso -> {
            log.info("Workflow in corso: {}", workflowInCorso);
            log.info("Id nuovo: {}", id);
            if (!workflowInCorso.getId().equals(id)) {
                throw new IllegalArgumentException("Terminare prima il workflow in esecuzione");
            }
        });
        engine.avviaWorkflow(id);
        return Response.noContent().build();
    }
}
