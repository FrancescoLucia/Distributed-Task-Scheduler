package it.unibas.taskscheduler.rest.dto;

import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.Workflow;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkflowDTO {

    private Long id;
    private String nome;
    private EStatoWorkflow stato;
    private LocalDateTime dataCreazione;
    private List<TaskDTO> tasks;

    public static WorkflowDTO from(Workflow workflow) {
        WorkflowDTO dto = new WorkflowDTO();
        dto.id = workflow.getId();
        dto.nome = workflow.getNome();
        dto.stato = workflow.getStato();
        dto.dataCreazione = workflow.getDataCreazione();
        dto.tasks = workflow.getTasks().stream().map(TaskDTO::from).toList();
        return dto;
    }
}
