package it.unibas.taskscheduler.rest.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class TaskImportDTO {

    private String nome;
    private String tipo;
    private JsonNode parametri;
    private List<String> dipendenze = new ArrayList<>();
}
