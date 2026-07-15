package it.unibas.taskscheduler.engine;

import it.unibas.taskscheduler.modello.ConfigurazioneEngine;
import it.unibas.taskscheduler.modello.EStatoTask;
import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.RetryPolicy;
import it.unibas.taskscheduler.modello.Task;
import it.unibas.taskscheduler.modello.Workflow;
import it.unibas.taskscheduler.observable.TaskObserver;
import it.unibas.taskscheduler.persistenza.IRepositoryConfigurazioneEngine;
import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
    IRepositoryConfigurazioneEngine repositoryConfigurazione;

    private static final RetryPolicy RetryPolicyDefault = new RetryPolicy(5, 5);

    @Transactional
    public void importaWorkflow(Workflow workflow) {
        assert workflow != null;
        log.info("Importazione workflow '{}'", workflow.getNome());
        workflow.setStato(EStatoWorkflow.IN_PAUSA);
        workflow.inizializzaFigli(this);
        repositoryWorkflow.persist(workflow);
        log.info("Workflow '{} - {}' importato con {} task.", workflow.getId(),  workflow.getNome(), workflow.getTasks().size());
    }

    @Transactional
    public void avviaWorkflow(Workflow workflow) {
        assert workflow != null;
        log.info("Avvio workflow '{}'", workflow.getNome());
        checkCambioStatoValido(workflow.getStato(), EStatoWorkflow.IN_ESECUZIONE);
        workflow.inizializzaRuntime(this);
        workflow.avvia();
        schedulaTaskWorkflow(workflow);
    }

    private void schedulaTaskWorkflow(Workflow workflow) {
        workflow.getTasks().stream()
            .filter(t -> t.getStato() == EStatoTask.PRONTO)
            .forEach(scheduler::schedulaTask);
    }

    @Override
    @Transactional
    public synchronized void aggiorna(Task task) {
        log.info("Aggiornamento task '{}': stato {}", task.getNome(), task.getStato());
        Workflow workflow = repositoryWorkflow.findByIdOptional(task.getWorkflowId()).orElseThrow(() -> new NotFoundException(String.format("Workflow %d non trovato per task '%d'", task.getWorkflowId(), task.getNome())));
        workflow.ricostruisciFigli();
        Task taskManaged = workflow.getTasks().stream()
                .filter(t -> t.getId().equals(task.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("Task %d non trovato nel workflow %d", task.getId(), workflow.getId())));
        if (taskManaged.getStato() != task.getStato()) {
            taskManaged.setStato(task.getStato());
        }
        taskManaged.setTentativi(task.getTentativi());

        EStatoWorkflow statoWorkflow = workflow.getStato();
        if (statoWorkflow == EStatoWorkflow.COMPLETATO || statoWorkflow == EStatoWorkflow.FALLITO || statoWorkflow == EStatoWorkflow.ANNULLATO) {
            return;
        }

        if (taskManaged.getStato().equals(EStatoTask.COMPLETATO)) {
            taskManaged.getFigli().forEach(figlio -> {
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

        } else if (taskManaged.getStato() == EStatoTask.FALLITO) {
            RetryPolicy policy = repositoryConfigurazione.find()
                    .map(ConfigurazioneEngine::getRetryPolicy)
                    .orElse(RetryPolicyDefault);

            if (taskManaged.getTentativi() < policy.getMaxTentativi()) {
                taskManaged.incrementaTentativi();
                log.warn("Task '{}' fallito. Retry {}/{} tra {} secondi.",
                        taskManaged.getNome(), taskManaged.getTentativi(), policy.getMaxTentativi(), policy.getIntervallo());
                taskManaged.setStato(EStatoTask.PRONTO);
                scheduler.schedulaTaskConRitardo(taskManaged, policy.getIntervallo());
            } else {
                log.error("Task '{}' fallito definitivamente dopo {} tentativi. Workflow {} FALLITO.",
                        taskManaged.getNome(), taskManaged.getTentativi(), workflow.getId());
                workflow.setStato(EStatoWorkflow.FALLITO);
                workflow.trasmettiStatoAiTask(EStatoTask.FALLITO);
            }
        }
    }

    @Transactional
    public synchronized void annullaWorkflow(Workflow workflow) {
        workflow.inizializzaRuntime(this);
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
