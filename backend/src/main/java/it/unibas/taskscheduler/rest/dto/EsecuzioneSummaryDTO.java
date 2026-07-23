package it.unibas.taskscheduler.rest.dto;

import it.unibas.taskscheduler.modello.EStatoTask;
import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.EsecuzioneWorkflow;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EsecuzioneSummaryDTO {

    private Long id;
    private Long workflowId;
    private String nome;
    private LocalDateTime dataInizio;
    private LocalDateTime dataFine;
    private EStatoWorkflow stato;
    private int taskTotali;
    private long taskCompletati;

    public static EsecuzioneSummaryDTO from(EsecuzioneWorkflow esecuzione) {
        EsecuzioneSummaryDTO dto = new EsecuzioneSummaryDTO();
        dto.id = esecuzione.getId();
        dto.workflowId = esecuzione.getWorkflow() != null ? esecuzione.getWorkflow().getId() : null;
        dto.nome = esecuzione.getNome();
        dto.dataInizio = esecuzione.getDataInizio();
        dto.dataFine = esecuzione.getDataFine();
        dto.stato = esecuzione.getStato();
        dto.taskTotali = esecuzione.getTasks().size();
        dto.taskCompletati = esecuzione.getTasks().stream()
                .filter(t -> t.getStato() == EStatoTask.COMPLETATO)
                .count();
        return dto;
    }
}
