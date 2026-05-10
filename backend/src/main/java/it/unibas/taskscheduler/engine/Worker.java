package it.unibas.taskscheduler.engine;

import it.unibas.taskscheduler.modello.EStatoTask;
import it.unibas.taskscheduler.modello.EStatoWorker;
import it.unibas.taskscheduler.modello.Task;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Data
public class Worker implements Runnable {

    private final Task task;
    private EStatoWorker stato;

    public Worker(Task task) {
        this.task = task;
    }

    @Override
    public void run() {
        log.info("Avvio task {}", task.getNome());
        this.stato = EStatoWorker.OCCUPATO;
        task.setStato(EStatoTask.IN_ESECUZIONE);
        try {
            double probabilitaFallimento = ConfigProvider.getConfig()
                    .getOptionalValue("task.scheduler.debug.probabilita-fallimento", Double.class)
                    .orElse(0.0);
            if (probabilitaFallimento > 0.0 && ThreadLocalRandom.current().nextDouble() < probabilitaFallimento) {
                throw new RuntimeException("[DEBUG] Fallimento simulato per il task " + task.getNome());
            }
            task.getAzione().esegui();
            task.setStato(EStatoTask.COMPLETATO);
            log.info("Task {} completato con successo.", task.getNome());
        } catch (Exception e) {
            log.error("Task {} fallito.", task.getNome(), e);
            try {
                task.setStato(EStatoTask.FALLITO);
            } catch (IllegalArgumentException ignored) {
                log.warn("Task {} già in stato {}, transizione a FALLITO ignorata.", task.getNome(), task.getStato());
            }
        } finally {
            this.stato = EStatoWorker.LIBERO;
        }
    }
}
