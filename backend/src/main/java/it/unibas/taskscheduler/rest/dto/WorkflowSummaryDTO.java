package it.unibas.taskscheduler.rest.dto;

import it.unibas.taskscheduler.modello.EStatoTask;
import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.Workflow;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkflowSummaryDTO {

    private Long id;
    private String nome;
    private EStatoWorkflow stato;
    private LocalDateTime dataCreazione;
    private int taskTotali;
    private long taskCompletati;

    public static WorkflowSummaryDTO from(Workflow workflow) {
        WorkflowSummaryDTO dto = new WorkflowSummaryDTO();
        dto.id = workflow.getId();
        dto.nome = workflow.getNome();
        dto.stato = workflow.getStato();
        dto.dataCreazione = workflow.getDataCreazione();
        dto.taskTotali = workflow.getTasks().size();
        dto.taskCompletati = workflow.getTasks().stream()
                .filter(t -> t.getStato() == EStatoTask.COMPLETATO)
                .count();
        return dto;
    }
}
