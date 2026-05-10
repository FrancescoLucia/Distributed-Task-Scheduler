package it.unibas.taskscheduler.engine;

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
        log.info("Importazione workflow '{}'", workflow.getNome());
        workflow.setStato(EStatoWorkflow.IN_PAUSA);
        repositoryWorkflow.persist(workflow);

        workflow.getTasks().forEach(task -> {
            task.setWorkflowId(workflow.getId());
            repositoryTask.persist(task);
        });

        workflow.getTasks().forEach(task ->
                task.getDipendenze().forEach(parent -> parent.getFigli().add(task))
        );

        workflow.getTasks().forEach(task -> {
            if (task.getDipendenze().isEmpty()) {
                task.setStato(EStatoTask.PRONTO);
            }
            repositoryTask.persist(task);
        });

        workflow.getTasks().forEach(task -> task.aggiungiObserver(this));

        log.info("Workflow '{} - {}' importato con {} task. In attesa di avvio.", workflow.getId(),  workflow.getNome(), workflow.getTasks().size());
    }

    public void avviaWorkflow(Long workflowId) {
        Workflow workflow = repositoryWorkflow.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow non trovato: " + workflowId));

        log.info("Avvio workflow '{}'", workflow.getNome());
        workflow.setStato(EStatoWorkflow.IN_ESECUZIONE);
        repositoryWorkflow.persist(workflow);

        workflow.getTasks().stream()
                .filter(t -> t.getStato() == EStatoTask.PRONTO)
                .forEach(scheduler::schedulaTask);
    }

    @Override
    public synchronized void aggiorna(Task task) {
        log.info("Aggiornamento task '{}': stato {}", task.getNome(), task.getStato());
        repositoryTask.persist(task);

        Workflow workflow = repositoryWorkflow.findById(task.getWorkflowId()).orElse(null);
        if (workflow == null) {
            log.error("Workflow {} non trovato per task '{}'", task.getWorkflowId(), task.getNome());
            return;
        }

        EStatoWorkflow statoWf = workflow.getStato();
        if (statoWf == EStatoWorkflow.COMPLETATO || statoWf == EStatoWorkflow.FALLITO || statoWf == EStatoWorkflow.ANNULLATO) {
            return;
        }

        if (task.getStato() == EStatoTask.COMPLETATO) {
            task.getFigli().forEach(figlio -> {
                if (figlio.getStato() == EStatoTask.IN_ATTESA
                        && figlio.getDipendenze().stream().allMatch(d -> d.getStato() == EStatoTask.COMPLETATO)) {
                    log.info("Task '{}' pronto.", figlio.getNome());
                    figlio.setStato(EStatoTask.PRONTO);
                    scheduler.schedulaTask(figlio);
                }
            });

            boolean tuttiCompletati = workflow.getTasks().stream()
                    .allMatch(t -> t.getStato() == EStatoTask.COMPLETATO);
            if (tuttiCompletati) {
                workflow.setStato(EStatoWorkflow.COMPLETATO);
                repositoryWorkflow.persist(workflow);
                log.info("Workflow '{}' completato.", workflow.getNome());
            }

        } else if (task.getStato() == EStatoTask.FALLITO) {
            RetryPolicy policy = repositoryConfigurazione.find()
                    .map(ConfigurazioneEngine::getRetryPolicy)
                    .orElse(RetryPolicyDefault);

            if (policy != null && task.getTentativi() < policy.getMaxTentativi()) {
                task.incrementaTentativi();
                repositoryTask.persist(task);
                log.warn("Task '{}' fallito. Retry {}/{} tra {} secondi.",
                        task.getNome(), task.getTentativi(), policy.getMaxTentativi(), policy.getIntervallo());
                task.setStato(EStatoTask.PRONTO);
                scheduler.schedulaTaskConRitardo(task, policy.getIntervallo());
            } else {
                log.error("Task '{}' fallito definitivamente dopo {} tentativi. Workflow marcato FALLITO.",
                        task.getNome(), task.getTentativi());
                workflow.setStato(EStatoWorkflow.FALLITO);
                repositoryWorkflow.persist(workflow);
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
                .filter(t -> t.getStato() == EStatoTask.IN_ESECUZIONE)
                .forEach(t -> {
                    t.getAzione().annulla();
                    t.setStato(EStatoTask.ANNULLATO);
                });
        workflow.trasmettiStatoAiTask(EStatoTask.ANNULLATO);
    }
}
