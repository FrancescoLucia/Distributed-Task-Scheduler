package it.unibas.taskscheduler.persistenza;

import it.unibas.taskscheduler.modello.Workflow;
import java.util.Optional;

public interface IRepositoryWorkflow {


    public void persist(Workflow workflow);

    public Optional<Workflow> findById(Long id);

    public boolean esisteWorkflowInCorso();
}
