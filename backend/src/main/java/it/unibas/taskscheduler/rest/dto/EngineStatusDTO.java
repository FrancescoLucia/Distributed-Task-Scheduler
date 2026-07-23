package it.unibas.taskscheduler.rest.dto;

import it.unibas.taskscheduler.modello.EStatoEngine;
import it.unibas.taskscheduler.modello.EStatoTask;
import it.unibas.taskscheduler.modello.EsecuzioneWorkflow;
import lombok.Data;

@Data
public class EngineStatusDTO {

    private EStatoEngine stato;
    private Long esecuzioneInCorsoId;
    private Long workflowInCorsoId;
    private String workflowInCorsoNome;
    private long taskAttivi;
    private long taskCompletati;
    private long taskTotali;

    public static EngineStatusDTO inattivo() {
        EngineStatusDTO dto = new EngineStatusDTO();
        dto.stato = EStatoEngine.INATTIVO;
        return dto;
    }

    public static EngineStatusDTO from(EsecuzioneWorkflow esecuzione) {
        EngineStatusDTO dto = new EngineStatusDTO();
        dto.stato = EStatoEngine.daStatoWorkflow(esecuzione.getStato());
        dto.esecuzioneInCorsoId = esecuzione.getId();
        dto.workflowInCorsoId = esecuzione.getWorkflow() != null ? esecuzione.getWorkflow().getId() : null;
        dto.workflowInCorsoNome = esecuzione.getNome();
        dto.taskAttivi = esecuzione.getTasks().stream()
                .filter(t -> t.getStato() == EStatoTask.IN_ESECUZIONE)
                .count();
        dto.taskCompletati = esecuzione.getTasks().stream()
                .filter(t -> t.getStato() == EStatoTask.COMPLETATO)
                .count();
        dto.taskTotali = esecuzione.getTasks().size();
        return dto;
    }
}
