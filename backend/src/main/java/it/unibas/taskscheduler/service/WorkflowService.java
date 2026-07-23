package it.unibas.taskscheduler.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unibas.taskscheduler.command.CalcoloCommand;
import it.unibas.taskscheduler.command.Command;
import it.unibas.taskscheduler.command.CommandMapper;
import it.unibas.taskscheduler.command.FileIOCommand;
import it.unibas.taskscheduler.command.HttpRequestCommand;
import it.unibas.taskscheduler.command.ScriptCommand;
import it.unibas.taskscheduler.modello.Workflow;
import it.unibas.taskscheduler.persistenza.IRepositoryEsecuzione;
import it.unibas.taskscheduler.persistenza.IRepositoryWorkflow;
import it.unibas.taskscheduler.rest.dto.DefinizioneTaskDTO;
import it.unibas.taskscheduler.rest.dto.GraphDTO;
import it.unibas.taskscheduler.rest.dto.TaskImportDTO;
import it.unibas.taskscheduler.rest.dto.WorkflowCatalogoDTO;
import it.unibas.taskscheduler.rest.dto.WorkflowImportDTO;
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
                definizione("Task-A", new CalcoloCommand(new CalcoloCommand.Params("SOMMA_PRIMI", 200000)), List.of()),
                definizione("Task-B", new FileIOCommand(new FileIOCommand.Params("SCRIVI", "/tmp/task-b.txt", "output Task-B")), List.of("Task-A")),
                definizione("Task-C", new HttpRequestCommand(new HttpRequestCommand.Params("https://example.com", "GET", null)), List.of("Task-A")),
                definizione("Task-D", new FileIOCommand(new FileIOCommand.Params("LEGGI", "/tmp/task-b.txt", null)), List.of("Task-B", "Task-C")),
                definizione("Task-E", new ScriptCommand("sleep " + ThreadLocalRandom.current().nextInt(1, 60) + " && echo 'Task-E done'"), List.of("Task-A"))
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
    public Long importaWorkflow(String json) {
        WorkflowImportDTO importazione = leggiImport(json);
        List<String> errori = ValidazioneWorkflow.validate(importazione);
        if (!errori.isEmpty()) {
            throw new WorkflowNonValidoException(errori);
        }
        List<DefinizioneTaskDTO> definizione = importazione.getTasks().stream()
                .map(this::mappaTask)
                .toList();
        Workflow workflow = new Workflow();
        workflow.setNome(importazione.getNome());
        workflow.setDefinizioneJson(serializzaDefinizione(definizione));
        workflow.setNumeroTask(definizione.size());
        repositoryWorkflow.persist(workflow);
        log.info("Workflow '{} - {}' importato da JSON con {} task.", workflow.getId(), workflow.getNome(), workflow.getNumeroTask());
        return workflow.getId();
    }

    public String templateWorkflow() {
        return """
                {
                  "nome": "Mio Workflow",
                  "tasks": [
                    { "nome": "Task-A", "tipo": "MATH",
                      "parametri": { "operazione": "FIBONACCI", "n": 40 }, "dipendenze": [] },
                    { "nome": "Task-B", "tipo": "FILE",
                      "parametri": { "operazione": "SCRIVI", "path": "/tmp/task-b.txt", "contenuto": "output Task-B" },
                      "dipendenze": ["Task-A"] },
                    { "nome": "Task-C", "tipo": "HTTP",
                      "parametri": { "url": "https://example.com", "metodo": "GET" }, "dipendenze": ["Task-A"] },
                    { "nome": "Task-D", "tipo": "SCRIPT",
                      "parametri": { "comando": "echo fatto" }, "dipendenze": ["Task-B", "Task-C"] }
                  ]
                }
                """;
    }

    private WorkflowImportDTO leggiImport(String json) {
        try {
            return objectMapper.readValue(json, WorkflowImportDTO.class);
        } catch (Exception e) {
            throw new WorkflowNonValidoException(List.of("JSON non valido: " + e.getMessage()));
        }
    }

    private DefinizioneTaskDTO mappaTask(TaskImportDTO task) {
        String payload = mappaPayload(task);
        List<String> dipendenze = task.getDipendenze() != null ? List.copyOf(task.getDipendenze()) : List.of();
        return new DefinizioneTaskDTO(task.getNome(), task.getTipo(), payload, dipendenze);
    }

    private String mappaPayload(TaskImportDTO task) {
        JsonNode parametri = task.getParametri();
        if (CommandMapper.SCRIPT.equals(task.getTipo())) {
            JsonNode comando = parametri.get("comando");
            return comando != null ? comando.asText() : "";
        }
        try {
            return objectMapper.writeValueAsString(parametri);
        } catch (Exception e) {
            throw new RuntimeException("Errore serializzazione parametri task", e);
        }
    }

    @Transactional
    public void rinominaWorkflow(Long id, String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Il campo 'nome' è obbligatorio");
        }
        Workflow workflow = repositoryWorkflow.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Workflow non trovato: " + id));
        repositoryEsecuzione.getEsecuzioneInCorso().ifPresent(esecuzione -> {
            if (esecuzione.getWorkflow() != null && id.equals(esecuzione.getWorkflow().getId())) {
                throw new IllegalStateException("Il workflow ha un'esecuzione in corso");
            }
        });
        String nuovoNome = nome.trim();
        workflow.setNome(nuovoNome);
        repositoryEsecuzione.aggiornaNomeWorkflow(id, nuovoNome);
        log.info("Workflow '{}' rinominato in '{}'.", id, nuovoNome);
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

    private DefinizioneTaskDTO definizione(String nomeTask, Command azione, List<String> dipendenze) {
        return new DefinizioneTaskDTO(nomeTask, CommandMapper.typeOf(azione), CommandMapper.payloadOf(azione), List.copyOf(dipendenze));
    }
}
