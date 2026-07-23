package it.unibas.taskscheduler.engine;

import it.unibas.taskscheduler.modello.ConfigurazioneEngine;
import it.unibas.taskscheduler.modello.EStatoTask;
import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.EsecuzioneWorkflow;
import it.unibas.taskscheduler.modello.RetryPolicy;
import it.unibas.taskscheduler.modello.Task;
import it.unibas.taskscheduler.engine.scheduling.StrategiaSchedulazione;
import it.unibas.taskscheduler.observable.TaskObserver;
import it.unibas.taskscheduler.persistenza.IRepositoryConfigurazioneEngine;
import it.unibas.taskscheduler.persistenza.IRepositoryEsecuzione;
import it.unibas.taskscheduler.persistenza.IRepositoryTask;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ApplicationScoped
public class Engine implements TaskObserver {

    @Inject
    Scheduler scheduler;

    @Inject
    IRepositoryEsecuzione repositoryEsecuzione;

    @Inject
    IRepositoryTask repositoryTask;

    @Inject
    IRepositoryConfigurazioneEngine repositoryConfigurazione;

    private static final RetryPolicy RetryPolicyDefault = new RetryPolicy(5, 5);

    private final Map<Long, EsecuzioneWorkflow> esecuzioniAttive = new ConcurrentHashMap<>();

    @Transactional
    synchronized void recuperaEsecuzioniAttive(@Observes StartupEvent ev) {
        repositoryEsecuzione.getEsecuzioneInCorso().ifPresent(esecuzione -> {
            log.info("Ripristino esecuzione attiva '{}' (stato {}).", esecuzione.getNome(), esecuzione.getStato());
            esecuzione.inizializzaRuntime(this);
            esecuzioniAttive.put(esecuzione.getId(), esecuzione);
            esecuzione.getTasks().stream()
                    .filter(Task::isInEsecuzione)
                    .forEach(t -> t.setStato(EStatoTask.PRONTO));
            if (esecuzione.getStato() == EStatoWorkflow.IN_ESECUZIONE) {
                schedulaTaskEsecuzione(esecuzione);
            }
        });
    }

    public EStatoWorkflow statoEsecuzione(Long id) {
        EsecuzioneWorkflow esecuzione = esecuzioniAttive.get(id);
        return esecuzione != null ? esecuzione.getStato() : null;
    }

    @Transactional
    public synchronized void pausaEsecuzione(Long id) {
        EsecuzioneWorkflow esecuzione = esecuzioniAttive.get(id);
        if (esecuzione == null || esecuzione.getStato() != EStatoWorkflow.IN_ESECUZIONE) {
            throw new IllegalStateException("L'esecuzione non è in corso");
        }
        esecuzione.pausa();
        repositoryEsecuzione.aggiornaStato(esecuzione);
        log.info("Esecuzione '{}' messa in pausa.", esecuzione.getNome());
    }

    @Transactional
    public synchronized void riprendiEsecuzione(Long id) {
        EsecuzioneWorkflow esecuzione = esecuzioniAttive.get(id);
        if (esecuzione == null || esecuzione.getStato() != EStatoWorkflow.IN_PAUSA) {
            throw new IllegalStateException("L'esecuzione non è in pausa");
        }
        esecuzione.riprendi();
        repositoryEsecuzione.aggiornaStato(esecuzione);
        log.info("Esecuzione '{}' ripresa.", esecuzione.getNome());
    }

    @Transactional
    public synchronized void avviaEsecuzione(EsecuzioneWorkflow esecuzione, StrategiaSchedulazione strategia) {
        assert esecuzione != null;
        log.info("Avvio esecuzione '{}'", esecuzione.getNome());
        checkCambioStatoValido(esecuzione.getStato(), EStatoWorkflow.IN_ESECUZIONE);
        esecuzione.inizializzaRuntime(this);
        esecuzione.avvia();
        esecuzioniAttive.put(esecuzione.getId(), esecuzione);
        repositoryEsecuzione.aggiornaStato(esecuzione);
        scheduler.setStrategia(strategia);
        schedulaTaskEsecuzione(esecuzione);
    }

    private void schedulaTaskEsecuzione(EsecuzioneWorkflow esecuzione) {
        esecuzione.getTasks().stream()
            .filter(t -> t.getStato() == EStatoTask.PRONTO)
            .forEach(scheduler::schedulaTask);
    }

    @Override
    @Transactional
    public synchronized void aggiorna(Task task) {
        log.info("Aggiornamento task '{}': stato {}", task.getNome(), task.getStato());
        EsecuzioneWorkflow esecuzione = esecuzioniAttive.get(task.getEsecuzioneId());
        if (esecuzione == null) {
            return;
        }
        repositoryTask.aggiornaStato(task);

        EStatoWorkflow statoEsecuzione = esecuzione.getStato();
        if (statoEsecuzione == EStatoWorkflow.COMPLETATO || statoEsecuzione == EStatoWorkflow.FALLITO || statoEsecuzione == EStatoWorkflow.ANNULLATO) {
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

            boolean tuttiCompletati = esecuzione.getTasks().stream()
                    .allMatch(Task::isCompletato);
            if (tuttiCompletati) {
                esecuzione.setStato(EStatoWorkflow.COMPLETATO);
                terminaEsecuzione(esecuzione);
                log.info("Esecuzione '{}' completata.", esecuzione.getNome());
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
                log.error("Task '{}' fallito definitivamente dopo {} tentativi. Esecuzione {} FALLITA.",
                        task.getNome(), task.getTentativi(), esecuzione.getId());
                esecuzione.setStato(EStatoWorkflow.FALLITO);
                scheduler.svuotaCoda();
                annullaTaskInEsecuzione(esecuzione);
                esecuzione.trasmettiStatoAiTask(EStatoTask.FALLITO);
                terminaEsecuzione(esecuzione);
            }
        }
    }

    @Transactional
    public synchronized void annullaEsecuzione(EsecuzioneWorkflow esecuzione) {
        EsecuzioneWorkflow attiva = esecuzioniAttive.getOrDefault(esecuzione.getId(), esecuzione);
        attiva.inizializzaRuntime(this);
        attiva.annulla();
        scheduler.svuotaCoda();
        annullaTuttiTask(attiva);
        terminaEsecuzione(attiva);
    }

    private void terminaEsecuzione(EsecuzioneWorkflow esecuzione) {
        esecuzione.setDataFine(LocalDateTime.now());
        repositoryEsecuzione.aggiornaStato(esecuzione);
        esecuzioniAttive.remove(esecuzione.getId());
    }

    private void annullaTuttiTask(EsecuzioneWorkflow esecuzione) {
        esecuzione.getTasks().stream()
                .filter(Task::isInEsecuzione)
                .forEach(t -> t.getAzione().annulla());
        esecuzione.trasmettiStatoAiTask(EStatoTask.ANNULLATO);
    }

    private void annullaTaskInEsecuzione(EsecuzioneWorkflow esecuzione) {
        esecuzione.getTasks().stream()
                .filter(Task::isInEsecuzione)
                .forEach(t -> t.getAzione().annulla());
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
