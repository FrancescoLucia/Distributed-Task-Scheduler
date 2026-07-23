package it.unibas.taskscheduler.engine;

import it.unibas.taskscheduler.modello.EStatoTask;
import it.unibas.taskscheduler.modello.Task;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Data
public class Worker implements Runnable {

    private final Task task;

    public Worker(Task task) {
        this.task = task;
    }

    @Override
    public void run() {
        log.info("Avvio task {}", task.getNome());
        task.setStato(EStatoTask.IN_ESECUZIONE);
        try {
            double probabilitaFallimentoDebug = ConfigProvider.getConfig()
                    .getOptionalValue("task.scheduler.debug.probabilita-fallimento", Double.class)
                    .orElse(0.0);
            if (probabilitaFallimentoDebug > 0.0 && ThreadLocalRandom.current().nextDouble() < probabilitaFallimentoDebug) {
                throw new RuntimeException("[DEBUG] Fallimento simulato per il task " + task.getNome());
            }
            task.esegui();
            task.setStato(EStatoTask.COMPLETATO);
            log.info("Task {} completato con successo.", task.getNome());
        } catch (Exception e) {
            log.error("Task {} fallito.", task.getNome(), e);
            task.annulla();
            task.setErrore(messaggioErrore(e));
            task.setStato(EStatoTask.FALLITO);
        }
    }

    private String messaggioErrore(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }
}
