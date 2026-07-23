package it.unibas.taskscheduler.persistenza;

import it.unibas.taskscheduler.modello.Workflow;
import java.util.Collection;
import java.util.Optional;

public interface IRepositoryWorkflow {

    public void persist(Workflow workflow);

    public Optional<Workflow> findByIdOptional(Long id);

    public Collection<Workflow> findAll(String filtroNome);

    public void delete(Long id);
}
