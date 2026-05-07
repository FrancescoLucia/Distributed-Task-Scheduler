package it.unibas.taskscheduler.engine;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import it.unibas.taskscheduler.modello.Task;
import it.unibas.taskscheduler.persistenza.IRepositoryTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

@Slf4j
@ApplicationScoped
public class Scheduler {

    @ConfigProperty(name = "task.scheduler.worker.numero", defaultValue = "4")
    int numeroWorker;

    private ExecutorService threadPoolWorkers;
    private final BlockingQueue<Task> codaTaskPronti = new PriorityBlockingQueue<>(
            1, (t1, t2) -> Integer.compare(t2.getFigli().size(), t1.getFigli().size()));
    private volatile boolean inEsecuzione = true;
    private Thread threadScheduler;

    @Inject
    IRepositoryTask repositoryTask;

    void startup(@Observes StartupEvent ev) {
        String strategiaPersistenza = repositoryTask.getClass().getName().indexOf("Mock") != -1 ? "Mock" : "Hibernate";
        log.info("Progetto avviato con persistenza {}", strategiaPersistenza);
        log.info("Avvio dello scheduler con {} thread worker.", numeroWorker);
        threadPoolWorkers = Executors.newFixedThreadPool(numeroWorker);
        
        threadScheduler = new Thread(this::assegnaTask);
        threadScheduler.start();
    }

    void shutdown(@Observes ShutdownEvent ev) {
        log.info("Arresto dello scheduler.");
        inEsecuzione = false;
        threadScheduler.interrupt();
        threadPoolWorkers.shutdown();
    }

    public void schedulaTask(Task task) {
        log.info("Schedulazione task {}.", task.getNome());
        codaTaskPronti.offer(task);
    }

    private void assegnaTask() {
        while (inEsecuzione) {
            try {
                Task task = codaTaskPronti.take();
                log.info("Invio task {} a un esecutore.", task.getNome());
                threadPoolWorkers.submit(new Worker(task));
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.warn("Thread dello scheduler interrotto.");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
