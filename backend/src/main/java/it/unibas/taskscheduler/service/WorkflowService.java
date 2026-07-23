package it.unibas.taskscheduler.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unibas.taskscheduler.command.CommandMapper;
import it.unibas.taskscheduler.command.ScriptCommand;
import it.unibas.taskscheduler.modello.Workflow;
import it.unibas.taskscheduler.persistenza.IRepositoryEsecuzione;
import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import it.unibas.taskscheduler.rest.dto.DefinizioneTaskDTO;
import it.unibas.taskscheduler.rest.dto.GraphDTO;
import it.unibas.taskscheduler.rest.dto.WorkflowCatalogoDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
@Slf4j
public class WorkflowService {

    @Inject
    IRepositoryWorkflow repositoryWorkflow;

    @Inject
    IRepositoryEsecuzione repositoryEsecuzione;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public List<WorkflowCatalogoDTO> listaWorkflow(String nome) {
        return repositoryWorkflow.findAll(nome).stream()
                .map(WorkflowCatalogoDTO::from)
                .toList();
    }

    @Transactional
    public GraphDTO getGrafo(Long id) {
        Workflow workflow = repositoryWorkflow.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Workflow non trovato: " + id));
        return GraphDTO.fromDefinizione(deserializzaDefinizione(workflow.getDefinizioneJson()));
    }

    @Transactional
    public Long importaWorkflowDemo() {
        List<DefinizioneTaskDTO> definizione = List.of(
                getTaskDemo("Task-A", List.of()),
                getTaskDemo("Task-B", List.of("Task-A")),
                getTaskDemo("Task-C", List.of("Task-A")),
                getTaskDemo("Task-D", List.of("Task-B", "Task-C")),
                getTaskDemo("Task-E", List.of("Task-A"))
        );
        Workflow workflow = new Workflow();
        workflow.setNome("Workflow Demo " + ThreadLocalRandom.current().nextInt(1, 100));
        workflow.setDefinizioneJson(serializzaDefinizione(definizione));
        workflow.setNumeroTask(definizione.size());
        repositoryWorkflow.persist(workflow);
        log.info("Workflow '{} - {}' importato con {} task.", workflow.getId(), workflow.getNome(), workflow.getNumeroTask());
        return workflow.getId();
    }

    @Transactional
    public void eliminaWorkflow(Long id) {
        Workflow workflow = repositoryWorkflow.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Workflow non trovato: " + id));
        repositoryEsecuzione.getEsecuzioneInCorso().ifPresent(esecuzione -> {
            if (esecuzione.getWorkflow() != null && id.equals(esecuzione.getWorkflow().getId())) {
                throw new IllegalStateException("Il workflow ha un'esecuzione in corso");
            }
        });
        repositoryWorkflow.delete(workflow.getId());
        log.info("Workflow '{}' eliminato dal catalogo.", workflow.getNome());
    }

    public String serializzaDefinizione(List<DefinizioneTaskDTO> definizione) {
        try {
            return objectMapper.writeValueAsString(definizione);
        } catch (Exception e) {
            throw new RuntimeException("Errore serializzazione definizione workflow", e);
        }
    }

    public List<DefinizioneTaskDTO> deserializzaDefinizione(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<DefinizioneTaskDTO>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Errore lettura definizione workflow", e);
        }
    }

    private DefinizioneTaskDTO getTaskDemo(String nomeTask, List<String> dipendenze) {
        int sleep = ThreadLocalRandom.current().nextInt(1, 120);
        String istruzione = String.format("sleep %d && echo '%s done'", sleep, nomeTask);
        ScriptCommand azione = new ScriptCommand(istruzione);
        return new DefinizioneTaskDTO(nomeTask, CommandMapper.typeOf(azione), CommandMapper.payloadOf(azione), List.copyOf(dipendenze));
    }
}
