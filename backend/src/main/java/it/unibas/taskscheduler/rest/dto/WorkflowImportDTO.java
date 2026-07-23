package it.unibas.taskscheduler.rest.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class WorkflowImportDTO {

    private String nome;
    private List<TaskImportDTO> tasks = new ArrayList<>();
}
