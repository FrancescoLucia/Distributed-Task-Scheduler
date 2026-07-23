package it.unibas.taskscheduler.persistenza.mock;

import it.unibas.taskscheduler.Utilita;
import it.unibas.taskscheduler.modello.Workflow;
import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.Comparator;
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
    public Optional<Workflow> findByIdOptional(Long id) {
        return Optional.ofNullable(workflows.get(id));
    }

    @Override
    public Collection<Workflow> findAll(String filtroNome) {
        return workflows.values().stream()
                .filter(w -> filtroNome == null || filtroNome.isBlank()
                        || w.getNome().toLowerCase().contains(filtroNome.toLowerCase()))
                .sorted(Comparator.comparing(Workflow::getDataCreazione).reversed())
                .toList();
    }

    @Override
    public void delete(Long id) {
        workflows.remove(id);
    }
}
