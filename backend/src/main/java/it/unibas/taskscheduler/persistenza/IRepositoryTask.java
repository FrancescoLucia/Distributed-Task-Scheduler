package it.unibas.taskscheduler.persistenza;

import it.unibas.taskscheduler.modello.Task;
import java.util.Optional;

public interface IRepositoryTask {

    public void persist(Task task);

    public void update(Task task);

    public Optional<Task> findById(Long id);
}
