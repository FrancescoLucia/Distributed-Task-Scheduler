package it.unibas.taskscheduler.rest.dto;

import it.unibas.taskscheduler.modello.Workflow;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkflowCatalogoDTO {

    private Long id;
    private String nome;
    private LocalDateTime dataCreazione;
    private int numeroTask;

    public static WorkflowCatalogoDTO from(Workflow workflow) {
        WorkflowCatalogoDTO dto = new WorkflowCatalogoDTO();
        dto.id = workflow.getId();
        dto.nome = workflow.getNome();
        dto.dataCreazione = workflow.getDataCreazione();
        dto.numeroTask = workflow.getNumeroTask();
        return dto;
    }
}
