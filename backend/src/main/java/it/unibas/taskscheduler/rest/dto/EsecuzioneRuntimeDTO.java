package it.unibas.taskscheduler.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EsecuzioneRuntimeDTO {

    private EngineStatusDTO engineStatus;
    private EsecuzioneSummaryDTO esecuzione;
    private GraphDTO graph;
}
