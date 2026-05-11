package it.unibas.taskscheduler.rest.dto;

import it.unibas.taskscheduler.modello.EStatoTask;
import it.unibas.taskscheduler.modello.Task;
import it.unibas.taskscheduler.modello.Workflow;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GraphDTO {

    private List<NodoDTO> nodi;
    private List<DipendenzaDTO> dipendenze;

    @Data
    @AllArgsConstructor
    public static class NodoDTO {
        private Long id;
        private String nome;
        private EStatoTask stato;
        private int tentativi;
    }

    @Data
    @AllArgsConstructor
    public static class DipendenzaDTO {
        private Long sorgente;
        private Long destinazione;
    }

    public static GraphDTO from(Workflow workflow) {
        GraphDTO dto = new GraphDTO();
        dto.nodi = workflow.getTasks().stream()
                .map(t -> new NodoDTO(t.getId(), t.getNome(), t.getStato(), t.getTentativi()))
                .toList();

        List<DipendenzaDTO> dipendenzeDTO = new ArrayList<>();
        for (Task task : workflow.getTasks()) {
            for (Task dep : task.getDipendenze()) {
                dipendenzeDTO.add(new DipendenzaDTO(dep.getId(), task.getId()));
            }
        }
        dto.dipendenze = dipendenzeDTO;
        return dto;
    }
}
