package it.unibas.taskscheduler.engine;

import it.unibas.taskscheduler.modello.EStatoTask;
import it.unibas.taskscheduler.modello.EStatoWorker;
import it.unibas.taskscheduler.modello.Task;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

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
            task.getAzione().esegui();
            task.setStato(EStatoTask.COMPLETATO);
            log.info("Task {} completato con successo.", task.getNome());
        } catch (Exception e) {
            log.error("Task {} fallito.", task.getNome(), e);
            task.setStato(EStatoTask.FALLITO);
        } finally {
            this.stato = EStatoWorker.LIBERO;
        }
    }
}
