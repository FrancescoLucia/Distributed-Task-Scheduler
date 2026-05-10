package it.unibas.taskscheduler.service;

import it.unibas.taskscheduler.command.ScriptCommand;
import it.unibas.taskscheduler.engine.Engine;
import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.Task;
import it.unibas.taskscheduler.modello.Workflow;
import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import it.unibas.taskscheduler.rest.dto.GraphDTO;
import it.unibas.taskscheduler.rest.dto.WorkflowDTO;
import it.unibas.taskscheduler.rest.dto.WorkflowSummaryDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
@Slf4j
public class WorkflowService {

    @Inject
    Engine engine;

    @Inject
    IRepositoryWorkflow repositoryWorkflow;

    public List<WorkflowSummaryDTO> listaWorkflow() {
        return repositoryWorkflow.findAll().stream()
                .map(WorkflowSummaryDTO::from)
                .toList();
    }

    public WorkflowDTO getWorkflow(Long id) {
        return repositoryWorkflow.findById(id)
                .map(WorkflowDTO::from)
                .orElseThrow(() -> new NotFoundException("Workflow non trovato: " + id));
    }

    public GraphDTO getGrafo(Long id) {
        return repositoryWorkflow.findById(id)
                .map(GraphDTO::from)
                .orElseThrow(() -> new NotFoundException("Workflow non trovato: " + id));
    }

    private Task getTaskDemo(String nomeTask) {
        int sleep = ThreadLocalRandom.current().nextInt(1, 120);
        String istruzione = String.format("sleep %d && echo '%s done'", sleep, nomeTask);
        return new Task(nomeTask, new ScriptCommand(istruzione));
    }

    public Long importaWorkflowDemo() {
        Task taskA = getTaskDemo("Task-A");
        Task taskB = getTaskDemo("Task-B");
        Task taskC = getTaskDemo("Task-C");
        Task taskD = getTaskDemo("Task-D");
        Task taskE = getTaskDemo("Task-E");

        taskB.getDipendenze().add(taskA);
        taskC.getDipendenze().add(taskA);
        taskE.getDipendenze().add(taskA);
        taskD.getDipendenze().add(taskB);
        taskD.getDipendenze().add(taskC);

        Workflow workflow = new Workflow();
        workflow.setNome("Workflow Demo " + ThreadLocalRandom.current().nextInt(1, 100));
        workflow.aggiungiTask(taskA);
        workflow.aggiungiTask(taskB);
        workflow.aggiungiTask(taskC);
        workflow.aggiungiTask(taskD);
        workflow.aggiungiTask(taskE);

        engine.importaWorkflow(workflow);
        return workflow.getId();
    }

    public void avviaWorkflow(Long id) {
        repositoryWorkflow.getWorkflowInCorso().ifPresent(workflowInCorso -> {
            log.info("Workflow in corso: {}", workflowInCorso);
            if (!workflowInCorso.getId().equals(id)) {
                throw new IllegalArgumentException("Terminare prima il workflow in esecuzione");
            }
        });
        engine.avviaWorkflow(id);
    }

    public void pausaWorkflow(Long id) {
        Workflow workflow = repositoryWorkflow.findById(id)
                .orElseThrow(() -> new NotFoundException("Workflow non trovato: " + id));
        workflow.pausa();
        log.info("Workflow '{}' messo in pausa.", workflow.getNome());
    }

    public void riprendiWorkflow(Long id) {
        Workflow workflow = repositoryWorkflow.findById(id)
                .orElseThrow(() -> new NotFoundException("Workflow non trovato: " + id));
        workflow.riprendi();
        log.info("Workflow '{}' ripreso.", workflow.getNome());
    }

    public void annullaWorkflow(Long id) {
        Workflow workflow = repositoryWorkflow.findById(id)
                .orElseThrow(() -> new NotFoundException("Workflow non trovato: " + id));
        EStatoWorkflow stato = workflow.getStato();
        if (stato == EStatoWorkflow.COMPLETATO || stato == EStatoWorkflow.FALLITO || stato == EStatoWorkflow.ANNULLATO) {
            throw new IllegalStateException("Il workflow è già in stato terminale");
        }
        engine.annullaWorkflow(workflow);
        log.info("Workflow '{}' annullato.", workflow.getNome());
    }
}
