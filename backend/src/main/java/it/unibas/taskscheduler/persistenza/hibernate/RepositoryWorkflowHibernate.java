package it.unibas.taskscheduler.persistenza.hibernate;

import java.util.Collection;
import java.util.Optional;

import io.quarkus.arc.properties.IfBuildProperty;
import it.unibas.taskscheduler.modello.Workflow;
import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import jakarta.enterprise.context.ApplicationScoped;

@IfBuildProperty(name = "dao.strategy", stringValue = "hibernate")
@ApplicationScoped
public class RepositoryWorkflowHibernate implements IRepositoryWorkflow{

    @Override
    public void persist(Workflow workflow) {
        throw new UnsupportedOperationException("Unimplemented method 'salva'");
    }

    @Override
    public Optional<Workflow> findById(Long id) {
        throw new UnsupportedOperationException("Unimplemented method 'trovaPerId'");
    }

    @Override
    public Optional<Workflow> getWorkflowInCorso() {
        throw new UnsupportedOperationException("Unimplemented method 'getWorkflowInCorso'");
    }

    @Override
    public Collection<Workflow> findAll() {
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

}
