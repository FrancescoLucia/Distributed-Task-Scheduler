package it.unibas.taskscheduler.persistenza.mock;

import it.unibas.taskscheduler.Utilita;
import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.Workflow;
import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@DefaultBean
public class RepositoryWorkflowMock implements IRepositoryWorkflow {

    private final Map<Long, Workflow> workflows = new ConcurrentHashMap<>();

    @Override
    public void persist(Workflow workflow) {
        if (workflow.getId() == null) {
            workflow.setId(Utilita.generaIdRandomMock(workflows.keySet()));
        }
        workflows.put(workflow.getId(), workflow);
    }

    @Override
    public Optional<Workflow> findById(Long id) {
        return Optional.ofNullable(workflows.get(id));
    }

    @Override
    public Optional<Workflow> getWorkflowInCorso() {
        return workflows.values().stream()
                .filter(w -> w.getStato().equals(EStatoWorkflow.IN_ESECUZIONE) || w.getStato().equals(EStatoWorkflow.IN_PAUSA))
                .findAny();
    }

    @Override
    public Collection<Workflow> findAll() {
        return workflows.values();
    }
}
