package it.unibas.taskscheduler.persistenza.hibernate;

import java.util.Collection;
import java.util.Optional;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.Workflow;
import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@IfBuildProperty(name = "dao.strategy", stringValue = "hibernate")
@ApplicationScoped
public class RepositoryWorkflowHibernate implements IRepositoryWorkflow, PanacheRepository<Workflow> {

    @Override
    @Transactional
    public void persist(Workflow workflow) {
        if (workflow.getId() == null) {
            PanacheRepository.super.persist(workflow);
        }
    }

    @Override
    @Transactional
    public Optional<Workflow> findByIdOptional(Long id) {
        Optional<Workflow> workflow = find("""
                        select distinct w
                        from Workflow w
                        left join fetch w.tasks t
                        left join fetch t.dipendenze
                        where w.id = ?1
                        """, id)
                .firstResultOptional();
        return workflow;
    }

    @Override
    @Transactional
    public Optional<Workflow> getWorkflowInCorso() {
        Optional<Workflow> workflow = find("""
                        select distinct w
                        from Workflow w
                        left join fetch w.tasks t
                        left join fetch t.dipendenze
                        where w.stato in (?1, ?2)
                        order by w.dataCreazione asc, w.id asc
                        """, EStatoWorkflow.IN_ESECUZIONE, EStatoWorkflow.IN_PAUSA)
                .firstResultOptional();
        return workflow;
    }

    @Override
    @Transactional
    public Collection<Workflow> findAllWorkflows() {
        Collection<Workflow> workflows = list("""
                        select distinct w
                        from Workflow w
                        left join fetch w.tasks
                        order by w.dataCreazione desc, w.id desc
                        """);
        return workflows;
    }
}
