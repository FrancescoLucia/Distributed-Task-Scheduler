package it.unibas.taskscheduler.persistenza;

import it.unibas.taskscheduler.modello.EsecuzioneWorkflow;
import java.util.Collection;
import java.util.Optional;

public interface IRepositoryEsecuzione {

    public void persist(EsecuzioneWorkflow esecuzione);

    public void aggiornaStato(EsecuzioneWorkflow esecuzione);

    public void aggiornaNomeWorkflow(Long workflowId, String nome);

    public Optional<EsecuzioneWorkflow> findByIdOptional(Long id);

    public Optional<EsecuzioneWorkflow> getEsecuzioneInCorso();

    public Collection<EsecuzioneWorkflow> findAll(Long workflowId);
}
