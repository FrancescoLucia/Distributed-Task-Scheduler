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
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<Long, Workflow> workflowAttivi = new ConcurrentHashMap<>();

    @Transactional
    synchronized void recuperaWorkflowAttivi(@Observes StartupEvent ev) {
        repositoryWorkflow.getWorkflowInCorso().ifPresent(workflow -> {
            log.info("Ripristino workflow attivo '{}' (stato {}).", workflow.getNome(), workflow.getStato());
            workflow.inizializzaRuntime(this);
            workflowAttivi.put(workflow.getId(), workflow);
            workflow.getTasks().stream()
                    .filter(Task::isInEsecuzione)
                    .forEach(t -> t.setStato(EStatoTask.PRONTO));
            if (workflow.getStato() == EStatoWorkflow.IN_ESECUZIONE) {
                schedulaTaskWorkflow(workflow);
            }
        });
    }

    public EStatoWorkflow statoWorkflow(Long id) {
        Workflow workflow = workflowAttivi.get(id);
        return workflow != null ? workflow.getStato() : null;
    }

    @Transactional
    public synchronized void pausaWorkflow(Long id) {
        Workflow workflow = workflowAttivi.get(id);
        if (workflow == null || workflow.getStato() != EStatoWorkflow.IN_ESECUZIONE) {
            throw new IllegalStateException("Il workflow non è in esecuzione");
        }
        workflow.pausa();
        repositoryWorkflow.aggiornaStato(workflow);
        log.info("Workflow '{}' messo in pausa.", workflow.getNome());
    }

    @Transactional
    public synchronized void riprendiWorkflow(Long id) {
        Workflow workflow = workflowAttivi.get(id);
        if (workflow == null || workflow.getStato() != EStatoWorkflow.IN_PAUSA) {
            throw new IllegalStateException("Il workflow non è in pausa");
        }
        workflow.riprendi();
        repositoryWorkflow.aggiornaStato(workflow);
        log.info("Workflow '{}' ripreso.", workflow.getNome());
    }

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
    public synchronized void avviaWorkflow(Workflow workflow) {
        assert workflow != null;
        log.info("Avvio workflow '{}'", workflow.getNome());
        checkCambioStatoValido(workflow.getStato(), EStatoWorkflow.IN_ESECUZIONE);
        workflow.inizializzaRuntime(this);
        workflow.avvia();
        workflowAttivi.put(workflow.getId(), workflow);
        repositoryWorkflow.aggiornaStato(workflow);
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
        Workflow workflow = workflowAttivi.get(task.getWorkflowId());
        if (workflow == null) {
            return;
        }
        repositoryTask.aggiornaStato(task);

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
                repositoryWorkflow.aggiornaStato(workflow);
                workflowAttivi.remove(workflow.getId());
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
                repositoryWorkflow.aggiornaStato(workflow);
                workflow.trasmettiStatoAiTask(EStatoTask.FALLITO);
                workflowAttivi.remove(workflow.getId());
            }
        }
    }

    @Transactional
    public synchronized void annullaWorkflow(Workflow workflow) {
        Workflow attivo = workflowAttivi.getOrDefault(workflow.getId(), workflow);
        attivo.inizializzaRuntime(this);
        attivo.annulla();
        repositoryWorkflow.aggiornaStato(attivo);
        scheduler.svuotaCoda();
        annullaTuttiTask(attivo);
        workflowAttivi.remove(attivo.getId());
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
