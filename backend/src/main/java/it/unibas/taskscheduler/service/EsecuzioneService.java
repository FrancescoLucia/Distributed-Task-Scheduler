package it.unibas.taskscheduler.service;

import it.unibas.taskscheduler.command.CommandMapper;
import it.unibas.taskscheduler.engine.Engine;
import it.unibas.taskscheduler.engine.scheduling.EAlgoritmoSchedulazione;
import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.EsecuzioneWorkflow;
import it.unibas.taskscheduler.modello.Task;
import it.unibas.taskscheduler.modello.Workflow;
import it.unibas.taskscheduler.persistenza.IRepositoryEsecuzione;
import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import it.unibas.taskscheduler.rest.dto.DefinizioneTaskDTO;
import it.unibas.taskscheduler.rest.dto.EsecuzioneSummaryDTO;
import it.unibas.taskscheduler.rest.dto.GraphDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Slf4j
public class EsecuzioneService {

    @Inject
    Engine engine;

    @Inject
    IRepositoryWorkflow repositoryWorkflow;

    @Inject
    IRepositoryEsecuzione repositoryEsecuzione;

    @Inject
    WorkflowService workflowService;

    @Transactional
    public Long avvia(Long workflowId, String algoritmo) {
        repositoryEsecuzione.getEsecuzioneInCorso().ifPresent(inCorso -> {
            throw new IllegalStateException("Terminare prima l'esecuzione in corso");
        });
        EAlgoritmoSchedulazione algoritmoScelto = EAlgoritmoSchedulazione.valueOf(algoritmo);
        Workflow workflow = repositoryWorkflow.findByIdOptional(workflowId)
                .orElseThrow(() -> new NotFoundException("Workflow non trovato: " + workflowId));

        List<DefinizioneTaskDTO> definizione = workflowService.deserializzaDefinizione(workflow.getDefinizioneJson());
        EsecuzioneWorkflow esecuzione = costruisciEsecuzione(workflow, definizione);
        esecuzione.inizializzaFigli(engine);
        repositoryEsecuzione.persist(esecuzione);
        engine.avviaEsecuzione(esecuzione, algoritmoScelto.getStrategia());
        return esecuzione.getId();
    }

    private EsecuzioneWorkflow costruisciEsecuzione(Workflow workflow, List<DefinizioneTaskDTO> definizione) {
        EsecuzioneWorkflow esecuzione = new EsecuzioneWorkflow();
        esecuzione.setWorkflow(workflow);
        esecuzione.setNome(workflow.getNome());

        Map<String, Task> taskPerNome = new HashMap<>();
        for (DefinizioneTaskDTO def : definizione) {
            Task task = new Task(def.getNome(), CommandMapper.from(def.getCommandType(), def.getCommandPayload()));
            taskPerNome.put(def.getNome(), task);
            esecuzione.aggiungiTask(task);
        }
        for (DefinizioneTaskDTO def : definizione) {
            Task task = taskPerNome.get(def.getNome());
            def.getDipendenze().forEach(nomeDip -> task.getDipendenze().add(taskPerNome.get(nomeDip)));
        }
        return esecuzione;
    }

    public void pausa(Long esecuzioneId) {
        engine.pausaEsecuzione(esecuzioneId);
    }

    public void riprendi(Long esecuzioneId) {
        engine.riprendiEsecuzione(esecuzioneId);
    }

    @Transactional
    public void annulla(Long esecuzioneId) {
        EsecuzioneWorkflow esecuzione = repositoryEsecuzione.findByIdOptional(esecuzioneId)
                .orElseThrow(() -> new NotFoundException("Esecuzione non trovata: " + esecuzioneId));
        EStatoWorkflow stato = esecuzione.getStato();
        if (stato == EStatoWorkflow.COMPLETATO || stato == EStatoWorkflow.FALLITO || stato == EStatoWorkflow.ANNULLATO) {
            throw new IllegalStateException("L'esecuzione è già in stato terminale");
        }
        engine.annullaEsecuzione(esecuzione);
        log.info("Esecuzione '{}' annullata.", esecuzione.getNome());
    }

    @Transactional
    public List<EsecuzioneSummaryDTO> storia(Long workflowId) {
        return repositoryEsecuzione.findAll(workflowId).stream()
                .map(EsecuzioneSummaryDTO::from)
                .toList();
    }

    @Transactional
    public EsecuzioneSummaryDTO getEsecuzione(Long id) {
        return repositoryEsecuzione.findByIdOptional(id)
                .map(EsecuzioneSummaryDTO::from)
                .orElseThrow(() -> new NotFoundException("Esecuzione non trovata: " + id));
    }

    @Transactional
    public GraphDTO getGrafo(Long id) {
        EsecuzioneWorkflow esecuzione = repositoryEsecuzione.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Esecuzione non trovata: " + id));
        esecuzione.ricostruisciFigli();
        return GraphDTO.from(esecuzione);
    }
}
