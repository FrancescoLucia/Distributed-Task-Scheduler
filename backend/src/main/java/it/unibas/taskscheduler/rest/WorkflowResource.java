package it.unibas.taskscheduler.rest;

import it.unibas.taskscheduler.command.ScriptCommand;
import it.unibas.taskscheduler.engine.Engine;
import it.unibas.taskscheduler.modello.Task;
import it.unibas.taskscheduler.modello.Workflow;
import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.ThreadLocalRandom;

@Path("/workflow")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class WorkflowResource {

    @Inject
    Engine engine;

    @Inject
    IRepositoryWorkflow repositoryWorkflow;

    @POST
    @Path("/importa")
    public Response importa() {
        Task taskA = new Task("Task-A", new ScriptCommand("sleep 1 && echo 'Test 1 done'"));
        Task taskB = new Task("Task-B", new ScriptCommand("sleep 1 && echo 'Test 2 done'"));
        Task taskC = new Task("Task-C", new ScriptCommand("sleep 1 && echo 'Test 3 done'"));
        Task taskD = new Task("Task-D", new ScriptCommand("sleep 1 && echo 'Test 4 done'"));
        Task taskE = new Task("Task-E", new ScriptCommand("sleep 1 && echo 'Test 5 done'"));

        taskA.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        taskB.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        taskC.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        taskD.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        taskE.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));

        taskB.getDipendenze().add(taskA.getId());
        taskC.getDipendenze().add(taskA.getId());
        taskE.getDipendenze().add(taskA.getId());
        taskD.getDipendenze().add(taskB.getId());
        taskD.getDipendenze().add(taskC.getId());

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
        if (repositoryWorkflow.esisteWorkflowInCorso()) {
            throw new IllegalArgumentException("Terminare prima il workflow in esecuzione");
        }
        engine.avviaWorkflow(id);
        return Response.noContent().build();
    }
}
