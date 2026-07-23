package it.unibas.taskscheduler.persistenza.mock;

import it.unibas.taskscheduler.Utilita;
import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.EsecuzioneWorkflow;
import it.unibas.taskscheduler.persistenza.IRepositoryEsecuzione;
import it.unibas.taskscheduler.persistenza.IRepositoryTask;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@DefaultBean
public class RepositoryEsecuzioneMock implements IRepositoryEsecuzione {

    private final Map<Long, EsecuzioneWorkflow> esecuzioni = new ConcurrentHashMap<>();

    @Inject
    IRepositoryTask repositoryTask;

    @Override
    public void persist(EsecuzioneWorkflow esecuzione) {
        if (esecuzione.getId() == null) {
            esecuzione.setId(Utilita.generaIdRandomMock(esecuzioni.keySet()));
        }
        esecuzione.getTasks().forEach(repositoryTask::persist);
        esecuzioni.put(esecuzione.getId(), esecuzione);
    }

    @Override
    public void aggiornaStato(EsecuzioneWorkflow esecuzione) {
        esecuzioni.put(esecuzione.getId(), esecuzione);
    }

    @Override
    public void aggiornaNomeWorkflow(Long workflowId, String nome) {
        esecuzioni.values().stream()
                .filter(e -> e.getWorkflow() != null && workflowId.equals(e.getWorkflow().getId()))
                .forEach(e -> e.setNome(nome));
    }

    @Override
    public Optional<EsecuzioneWorkflow> findByIdOptional(Long id) {
        return Optional.ofNullable(esecuzioni.get(id));
    }

    @Override
    public Optional<EsecuzioneWorkflow> getEsecuzioneInCorso() {
        return esecuzioni.values().stream()
                .filter(e -> e.getStato().equals(EStatoWorkflow.IN_ESECUZIONE) || e.getStato().equals(EStatoWorkflow.IN_PAUSA))
                .findAny();
    }

    @Override
    public Collection<EsecuzioneWorkflow> findAll(Long workflowId) {
        return esecuzioni.values().stream()
                .filter(e -> workflowId == null
                        || (e.getWorkflow() != null && workflowId.equals(e.getWorkflow().getId())))
                .sorted(Comparator.comparing(EsecuzioneWorkflow::getDataInizio).reversed())
                .toList();
    }
}
