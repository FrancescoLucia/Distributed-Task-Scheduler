package it.unibas.taskscheduler.engine;

import io.quarkus.test.junit.QuarkusTest;
import it.unibas.taskscheduler.command.ScriptCommand;
import it.unibas.taskscheduler.engine.scheduling.FigliDecrescente;
import it.unibas.taskscheduler.modello.ConfigurazioneEngine;
import it.unibas.taskscheduler.modello.EStatoTask;
import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.EsecuzioneWorkflow;
import it.unibas.taskscheduler.modello.RetryPolicy;
import it.unibas.taskscheduler.modello.Task;
import it.unibas.taskscheduler.persistenza.IRepositoryConfigurazioneEngine;
import it.unibas.taskscheduler.persistenza.IRepositoryEsecuzione;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class EngineRegressionTest {

    @Inject
    Engine engine;

    @Inject
    IRepositoryEsecuzione repositoryEsecuzione;

    @Inject
    IRepositoryConfigurazioneEngine repositoryConfigurazione;

    @TempDir
    Path tempDir;

    private static final AtomicLong CONTATORE_ID = new AtomicLong(1);

    @BeforeEach
    void policyRetryVeloce() {
        repositoryConfigurazione.persist(new ConfigurazioneEngine(new RetryPolicy(2, 0)));
    }

    private EsecuzioneWorkflow avvia(Task... tasks) {
        EsecuzioneWorkflow esecuzione = new EsecuzioneWorkflow();
        esecuzione.setId(CONTATORE_ID.getAndIncrement());
        for (Task task : tasks) {
            esecuzione.aggiungiTask(task);
        }
        esecuzione.inizializzaFigli(engine);
        repositoryEsecuzione.persist(esecuzione);
        engine.avviaEsecuzione(esecuzione, new FigliDecrescente());
        return esecuzione;
    }

    private void attendiStatoFinale(EsecuzioneWorkflow esecuzione) {
        long scadenza = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < scadenza) {
            EStatoWorkflow stato = esecuzione.getStato();
            if (stato == EStatoWorkflow.COMPLETATO || stato == EStatoWorkflow.FALLITO || stato == EStatoWorkflow.ANNULLATO) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Test
    void workflowVieneCompletato() {
        Path markerB = tempDir.resolve("b.done");
        Path markerC = tempDir.resolve("c.done");

        Task a = new Task("A", new ScriptCommand("true"));
        Task b = new Task("B", new ScriptCommand("sleep 0.2 && touch '" + markerB + "'"));
        Task c = new Task("C", new ScriptCommand("sleep 0.2 && touch '" + markerC + "'"));
        Task d = new Task("D", new ScriptCommand("test -f '" + markerB + "' && test -f '" + markerC + "'"));
        b.getDipendenze().add(a);
        c.getDipendenze().add(a);
        d.getDipendenze().add(b);
        d.getDipendenze().add(c);

        EsecuzioneWorkflow esecuzione = avvia(a, b, c, d);
        attendiStatoFinale(esecuzione);

        assertEquals(EStatoWorkflow.COMPLETATO, esecuzione.getStato());
        assertEquals(EStatoTask.COMPLETATO, a.getStato());
        assertEquals(EStatoTask.COMPLETATO, b.getStato());
        assertEquals(EStatoTask.COMPLETATO, c.getStato());
        assertEquals(EStatoTask.COMPLETATO, d.getStato());
    }

    @Test
    void comportamentoDiRetryTask() {
        Path marker = tempDir.resolve("retry.marker");
        Task task = new Task("retry", new ScriptCommand(
                "if [ -f '" + marker + "' ]; then exit 0; else touch '" + marker + "'; exit 1; fi"));

        EsecuzioneWorkflow esecuzione = avvia(task);
        attendiStatoFinale(esecuzione);

        assertEquals(EStatoWorkflow.COMPLETATO, esecuzione.getStato());
        assertEquals(EStatoTask.COMPLETATO, task.getStato());
        assertEquals(1, task.getTentativi());
    }

    @Test
    void fallimentoInteroWorkflow() {
        Task task = new Task("sempreFallito", new ScriptCommand("exit 1"));
        EsecuzioneWorkflow esecuzione = avvia(task);
        attendiStatoFinale(esecuzione);

        assertEquals(EStatoWorkflow.FALLITO, esecuzione.getStato());
        assertEquals(EStatoTask.FALLITO, task.getStato());
        assertEquals(2, task.getTentativi());
    }

    @Test
    void transizioniStatoNonValide() {
        Task task = new Task("lento", new ScriptCommand("sleep 0.4"));
        EsecuzioneWorkflow esecuzione = avvia(task);

        engine.pausaEsecuzione(esecuzione.getId());
        assertEquals(EStatoWorkflow.IN_PAUSA, esecuzione.getStato());
        assertThrows(IllegalStateException.class, () -> engine.pausaEsecuzione(esecuzione.getId()));

        engine.riprendiEsecuzione(esecuzione.getId());
        assertEquals(EStatoWorkflow.IN_ESECUZIONE, esecuzione.getStato());
        assertThrows(IllegalStateException.class, () -> engine.riprendiEsecuzione(esecuzione.getId()));

        attendiStatoFinale(esecuzione);
        assertEquals(EStatoWorkflow.COMPLETATO, esecuzione.getStato());
    }

    @Test
    void annullamentoWorkflow() {
        Task a = new Task("A", new ScriptCommand("sleep 0.4"));
        Task b = new Task("B", new ScriptCommand("true"));
        b.getDipendenze().add(a);

        EsecuzioneWorkflow esecuzione = avvia(a, b);
        engine.annullaEsecuzione(esecuzione);

        assertEquals(EStatoWorkflow.ANNULLATO, esecuzione.getStato());
        assertEquals(EStatoTask.ANNULLATO, b.getStato());
    }
}
