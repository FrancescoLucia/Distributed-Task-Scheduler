package it.unibas.taskscheduler.engine;

import java.util.ArrayList;

import it.unibas.taskscheduler.modello.ConfigurazioneEngine;
import it.unibas.taskscheduler.modello.EStatoTask;
import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.RetryPolicy;
import it.unibas.taskscheduler.modello.Task;
import it.unibas.taskscheduler.modello.Workflow;
import it.unibas.taskscheduler.observable.TaskObserver;
import it.unibas.taskscheduler.persistenza.IRepositoryConfigurazioneEngine;
import it.unibas.taskscheduler.persistenza.IRepositoryTask;
import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class Engine implements TaskObserver {

    @Inject
    Scheduler scheduler;

    @Inject
    IRepositoryWorkflow repositoryWorkflow;

    @Inject
    IRepositoryTask repositoryTask;

    @Inject
    IRepositoryConfigurazioneEngine repositoryConfigurazione;

    private static final RetryPolicy RetryPolicyDefault = new RetryPolicy(5, 5);

    public void importaWorkflow(Workflow workflow) {
        assert workflow != null;
        log.info("Importazione workflow '{}'", workflow.getNome());
        workflow.setStato(EStatoWorkflow.IN_PAUSA);
        repositoryWorkflow.persist(workflow);

        workflow.getTasks().forEach(task -> {
            task.setWorkflowId(workflow.getId());
        });

        workflow.inizializzaFigli(this);
        workflow.getTasks().forEach(repositoryTask::persist);
        log.info("Workflow '{} - {}' importato con {} task.", workflow.getId(),  workflow.getNome(), workflow.getTasks().size());
    }

    public void avviaWorkflow(Workflow workflow) {
        assert workflow != null;
        log.info("Avvio workflow '{}'", workflow.getNome());
        checkCambioStatoValido(workflow.getStato(), EStatoWorkflow.IN_ESECUZIONE);
        workflow.avvia();
        schedulaTaskWorkflow(workflow);
    }

    private void schedulaTaskWorkflow(Workflow workflow) {
        workflow.getTasks().stream()
            .filter(t -> t.getStato() == EStatoTask.PRONTO)
            .forEach(scheduler::schedulaTask);
    }

    @Override
    public synchronized void aggiorna(Task task) {
        log.info("Aggiornamento task '{}': stato {}", task.getNome(), task.getStato());
        repositoryTask.persist(task);

        Workflow workflow = repositoryWorkflow.findById(task.getWorkflowId()).orElseThrow(() -> new NotFoundException(String.format("Workflow %d non trovato per task '%d'", task.getWorkflowId(), task.getNome())));

        EStatoWorkflow statoWorkflow = workflow.getStato();
        if (statoWorkflow == EStatoWorkflow.COMPLETATO || statoWorkflow == EStatoWorkflow.FALLITO || statoWorkflow == EStatoWorkflow.ANNULLATO) {
            return;
        }

        if (task.getStato().equals(EStatoTask.COMPLETATO)) {
            task.getFigli().forEach(figlio -> {
                if (figlio.getStato() == EStatoTask.IN_ATTESA
                        && figlio.getDipendenze().stream().allMatch(d -> d.getStato() == EStatoTask.COMPLETATO)) {
                    log.info("Task '{}' pronto.", figlio.getNome());
                    figlio.setStato(EStatoTask.PRONTO);
                    scheduler.schedulaTask(figlio);
                }
            });

            boolean tuttiCompletati = workflow.getTasks().stream()
                    .allMatch(Task::isCompletato);
            if (tuttiCompletati) {
                workflow.setStato(EStatoWorkflow.COMPLETATO);
                log.info("Workflow '{}' completato.", workflow.getNome());
            }

        } else if (task.getStato() == EStatoTask.FALLITO) {
            RetryPolicy policy = repositoryConfigurazione.find()
                    .map(ConfigurazioneEngine::getRetryPolicy)
                    .orElse(RetryPolicyDefault);

            if (task.getTentativi() < policy.getMaxTentativi()) {
                task.incrementaTentativi();
                log.warn("Task '{}' fallito. Retry {}/{} tra {} secondi.",
                        task.getNome(), task.getTentativi(), policy.getMaxTentativi(), policy.getIntervallo());
                task.setStato(EStatoTask.PRONTO);
                scheduler.schedulaTaskConRitardo(task, policy.getIntervallo());
            } else {
                log.error("Task '{}' fallito definitivamente dopo {} tentativi. Workflow {} FALLITO.",
                        task.getNome(), task.getTentativi(), workflow.getId());
                workflow.setStato(EStatoWorkflow.FALLITO);
                workflow.trasmettiStatoAiTask(EStatoTask.FALLITO);
            }
        }
    }

    public synchronized void annullaWorkflow(Workflow workflow) {
        workflow.annulla();
        scheduler.svuotaCoda();
        annullaTuttiTask(workflow);
    }

    private void annullaTuttiTask(Workflow workflow) {
        workflow.getTasks().stream()
                .filter(Task::isInEsecuzione)
                .forEach(t -> {
                    t.getAzione().annulla();
                });
        workflow.trasmettiStatoAiTask(EStatoTask.ANNULLATO);
    }

    private boolean checkCambioStatoValido(EStatoWorkflow vecchioStato, EStatoWorkflow nuovoStato) {
        if (nuovoStato.equals(EStatoWorkflow.IN_ESECUZIONE)) {
            return vecchioStato.equals(EStatoWorkflow.IN_PAUSA);
        }
        if (vecchioStato.equals(EStatoWorkflow.IN_ESECUZIONE)) {
            return nuovoStato.equals(EStatoWorkflow.FALLITO) ||
            nuovoStato.equals(EStatoWorkflow.IN_PAUSA) ||
            nuovoStato.equals(EStatoWorkflow.COMPLETATO) ||
            nuovoStato.equals(EStatoWorkflow.ANNULLATO);
        }
        return false;
    }
}
