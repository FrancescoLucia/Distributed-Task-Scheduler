package it.unibas.taskscheduler.rest.dto;

import it.unibas.taskscheduler.modello.EStatoTask;
import it.unibas.taskscheduler.modello.Task;
import lombok.Data;

import java.util.List;

@Data
public class TaskDTO {

    private Long id;
    private Long esecuzioneId;
    private String nome;
    private EStatoTask stato;
    private int tentativi;
    private List<Long> dipendenze;
    private List<Long> figli;

    public static TaskDTO from(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.id = task.getId();
        dto.esecuzioneId = task.getEsecuzioneId();
        dto.nome = task.getNome();
        dto.stato = task.getStato();
        dto.tentativi = task.getTentativi();
        dto.dipendenze = task.getDipendenze().stream().map(Task::getId).toList();
        dto.figli = task.getFigli().stream().map(Task::getId).toList();
        return dto;
    }
}
