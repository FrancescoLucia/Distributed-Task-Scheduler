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
                .map(t -> new NodoDTO(t.getId(), t.getNome(), t.getStato()))
                .toList();

        List<DipendenzaDTO> edges = new ArrayList<>();
        for (Task task : workflow.getTasks()) {
            for (Task dep : task.getDipendenze()) {
                edges.add(new DipendenzaDTO(dep.getId(), task.getId()));
            }
        }
        dto.dipendenze = edges;
        return dto;
    }
}
