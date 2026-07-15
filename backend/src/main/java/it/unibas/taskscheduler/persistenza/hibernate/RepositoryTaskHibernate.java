package it.unibas.taskscheduler.persistenza.hibernate;

import java.util.Optional;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import it.unibas.taskscheduler.modello.Task;
import it.unibas.taskscheduler.persistenza.IRepositoryTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@IfBuildProperty(name = "dao.strategy", stringValue = "hibernate")
@ApplicationScoped
public class RepositoryTaskHibernate implements IRepositoryTask, PanacheRepository<Task> {

    @Override
    @Transactional
    public void persist(Task task) {
        if (task.getId() == null) {
            PanacheRepository.super.persist(task);
        }
    }

    @Override
    @Transactional
    public Optional<Task> findByIdOptional(Long id) {
        return find("""
                        select distinct t
                        from Task t
                        left join fetch t.workflow w
                        left join fetch t.dipendenze
                        where t.id = ?1
                        """, id)
                .firstResultOptional();
    }

}
