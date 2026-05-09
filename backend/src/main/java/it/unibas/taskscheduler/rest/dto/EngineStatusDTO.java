package it.unibas.taskscheduler.rest.dto;

import it.unibas.taskscheduler.modello.EStatoTask;
import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.Workflow;
import lombok.Data;

@Data
public class EngineStatusDTO {

    private EStatoWorkflow stato;
    private Long workflowInCorsoId;
    private String workflowInCorsoNome;
    private long taskAttivi;
    private long taskCompletati;
    private long taskTotali;

    public static EngineStatusDTO inattivo() {
        EngineStatusDTO dto = new EngineStatusDTO();
        dto.stato = null;
        return dto;
    }

    public static EngineStatusDTO from(Workflow workflow) {
        EngineStatusDTO dto = new EngineStatusDTO();
        dto.stato = workflow.getStato();
        dto.workflowInCorsoId = workflow.getId();
        dto.workflowInCorsoNome = workflow.getNome();
        dto.taskAttivi = workflow.getTasks().stream()
                .filter(t -> t.getStato() == EStatoTask.IN_ESECUZIONE)
                .count();
        dto.taskCompletati = workflow.getTasks().stream()
                .filter(t -> t.getStato() == EStatoTask.COMPLETATO)
                .count();
        dto.taskTotali = workflow.getTasks().size();
        return dto;
    }
}
