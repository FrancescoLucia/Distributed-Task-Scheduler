package it.unibas.taskscheduler.persistenza.mock;

import it.unibas.taskscheduler.Utilita;
import it.unibas.taskscheduler.modello.Task;
import it.unibas.taskscheduler.persistenza.IRepositoryTask;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@DefaultBean
public class RepositoryTaskMock implements IRepositoryTask {

    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();

    @Override
    public void persist(Task task) {
        if (task.getId() == null) {
            task.setId(Utilita.generaIdRandomMock(tasks.keySet()));
        }
        tasks.put(task.getId(), task);
    }

    @Override
    public void aggiornaStato(Task task) {
        tasks.put(task.getId(), task);
    }

    @Override
    public Optional<Task> findByIdOptional(Long id) {
        return Optional.ofNullable(tasks.get(id));
    }

}
