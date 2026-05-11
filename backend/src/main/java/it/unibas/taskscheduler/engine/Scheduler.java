package it.unibas.taskscheduler.engine;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import it.unibas.taskscheduler.Utilita;
import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.Task;
import it.unibas.taskscheduler.modello.Workflow;
import it.unibas.taskscheduler.persistenza.IRepositoryTask;
import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@ApplicationScoped
public class Scheduler {

    @ConfigProperty(name = "task.scheduler.worker.numero", defaultValue = "4")
    int numeroWorker;

    private ExecutorService threadPoolWorkers;
    private ScheduledExecutorService scheduledExecutor;
    private final BlockingQueue<Task> codaTaskPronti = new PriorityBlockingQueue<>(
            10, Utilita::comparaTaskPerNumeroFigli);
    private volatile boolean inEsecuzione = true;
    private Thread threadScheduler;

    @Inject
    IRepositoryTask repositoryTask;

    @Inject
    IRepositoryWorkflow repositoryWorkflow;

    void startup(@Observes StartupEvent ev) {
        String strategiaPersistenza = repositoryTask.getClass().getName().indexOf("Mock") != -1 ? "Mock" : "Hibernate";
        log.info("Progetto avviato con persistenza {}", strategiaPersistenza);
        log.info("Avvio dello scheduler con {} thread worker.", numeroWorker);
        threadPoolWorkers = Executors.newFixedThreadPool(numeroWorker);
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

        threadScheduler = new Thread(this::assegnaTask);
        threadScheduler.start();
    }

    void shutdown(@Observes ShutdownEvent ev) {
        log.info("Arresto dello scheduler.");
        inEsecuzione = false;
        threadScheduler.interrupt();
        threadPoolWorkers.shutdown();
        scheduledExecutor.shutdown();
    }

    public void schedulaTask(Task task) {
        log.info("Schedulazione task {}.", task.getNome());
        codaTaskPronti.offer(task);
    }

    public void schedulaTaskConRitardo(Task task, int intervalloSecondi) {
        scheduledExecutor.schedule(() -> schedulaTask(task), intervalloSecondi, TimeUnit.SECONDS);
    }

    public void svuotaCoda() {
        codaTaskPronti.clear();
    }
    

    private void assegnaTask() {
        while (inEsecuzione) {
            try {
                Task task = codaTaskPronti.poll(500, TimeUnit.MILLISECONDS);
                if (task == null) continue;

                Workflow workflow = repositoryWorkflow.findById(task.getWorkflowId()).orElse(null);
                if (workflow == null) continue;

                EStatoWorkflow statoWf = workflow.getStato();
                if (statoWf == EStatoWorkflow.IN_PAUSA) {
                    codaTaskPronti.offer(task);
                    Thread.sleep(200);
                    continue;
                }
                if (statoWf != EStatoWorkflow.IN_ESECUZIONE) {
                    continue;
                }
                repositoryTask.update(task);
                log.info("Invio task {} a un executor.", task.getNome());
                threadPoolWorkers.submit(new Worker(task));
            } catch (InterruptedException e) {
                log.warn("Thread dello scheduler interrotto.");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
