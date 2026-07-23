package it.unibas.taskscheduler.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefinizioneTaskDTO {

    private String nome;
    private String commandType;
    private String commandPayload;
    private List<String> dipendenze = new ArrayList<>();
}
