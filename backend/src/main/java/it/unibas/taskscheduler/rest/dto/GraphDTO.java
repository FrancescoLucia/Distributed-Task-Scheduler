package it.unibas.taskscheduler.rest.dto;

import it.unibas.taskscheduler.modello.EStatoTask;
import it.unibas.taskscheduler.modello.EsecuzioneWorkflow;
import it.unibas.taskscheduler.modello.Task;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class GraphDTO {

    private List<NodoDTO> nodi;
    private List<DipendenzaDTO> dipendenze;

    @Data
    @AllArgsConstructor
    public static class NodoDTO {
        private Long id;
        private String nome;
        private String tipo;
        private EStatoTask stato;
        private int tentativi;
    }

    @Data
    @AllArgsConstructor
    public static class DipendenzaDTO {
        private Long sorgente;
        private Long destinazione;
    }

    public static GraphDTO from(EsecuzioneWorkflow esecuzione) {
        GraphDTO dto = new GraphDTO();
        dto.nodi = esecuzione.getTasks().stream()
                .map(t -> new NodoDTO(t.getId(), t.getNome(), t.getCommandType(), t.getStato(), t.getTentativi()))
                .toList();

        List<DipendenzaDTO> dipendenzeDTO = new ArrayList<>();
        for (Task task : esecuzione.getTasks()) {
            for (Task dep : task.getDipendenze()) {
                dipendenzeDTO.add(new DipendenzaDTO(dep.getId(), task.getId()));
            }
        }
        dto.dipendenze = dipendenzeDTO;
        return dto;
    }

    public static GraphDTO fromDefinizione(List<DefinizioneTaskDTO> definizione) {
        GraphDTO dto = new GraphDTO();
        Map<String, Long> idPerNome = new HashMap<>();
        long indice = 0;
        for (DefinizioneTaskDTO def : definizione) {
            idPerNome.put(def.getNome(), indice++);
        }
        dto.nodi = definizione.stream()
                .map(def -> new NodoDTO(idPerNome.get(def.getNome()), def.getNome(), def.getCommandType(), null, 0))
                .toList();

        List<DipendenzaDTO> dipendenzeDTO = new ArrayList<>();
        for (DefinizioneTaskDTO def : definizione) {
            for (String dep : def.getDipendenze()) {
                dipendenzeDTO.add(new DipendenzaDTO(idPerNome.get(dep), idPerNome.get(def.getNome())));
            }
        }
        dto.dipendenze = dipendenzeDTO;
        return dto;
    }
}
