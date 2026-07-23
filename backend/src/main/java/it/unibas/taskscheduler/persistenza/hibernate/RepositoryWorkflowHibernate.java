package it.unibas.taskscheduler.persistenza.hibernate;

import java.util.Collection;
import java.util.Optional;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
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
        return find("id", id).firstResultOptional();
    }

    @Override
    @Transactional
    public Collection<Workflow> findAll(String filtroNome) {
        if (filtroNome == null || filtroNome.isBlank()) {
            return list("order by dataCreazione desc, id desc");
        }
        return list("lower(nome) like lower(?1) order by dataCreazione desc, id desc", "%" + filtroNome + "%");
    }

    @Override
    @Transactional
    public void delete(Long id) {
        deleteById(id);
    }
}
