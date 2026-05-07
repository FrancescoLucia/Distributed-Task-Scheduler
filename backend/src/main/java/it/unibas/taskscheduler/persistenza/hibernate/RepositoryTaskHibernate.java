package it.unibas.taskscheduler.persistenza.hibernate;

import java.util.Optional;

import io.quarkus.arc.properties.IfBuildProperty;
import it.unibas.taskscheduler.modello.Task;
import it.unibas.taskscheduler.persistenza.IRepositoryTask;
import jakarta.enterprise.context.ApplicationScoped;

@IfBuildProperty(name = "dao.strategy", stringValue = "hibernate")
@ApplicationScoped
public class RepositoryTaskHibernate implements IRepositoryTask {

    @Override
    public void persist(Task task) {
        throw new UnsupportedOperationException("Unimplemented method 'salva'");
    }

    @Override
    public Optional<Task> findById(Long id) {
        throw new UnsupportedOperationException("Unimplemented method 'trovaPerId'");
    }

}
