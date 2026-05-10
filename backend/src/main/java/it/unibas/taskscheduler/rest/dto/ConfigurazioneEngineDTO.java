package it.unibas.taskscheduler.rest.dto;

import it.unibas.taskscheduler.modello.ConfigurazioneEngine;
import it.unibas.taskscheduler.modello.RetryPolicy;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConfigurazioneEngineDTO {

    private int maxTentativi;
    private int intervallo;

    public static ConfigurazioneEngineDTO from(ConfigurazioneEngine config) {
        ConfigurazioneEngineDTO dto = new ConfigurazioneEngineDTO();
        RetryPolicy policy = config.getRetryPolicy();
        if (policy != null) {
            dto.maxTentativi = policy.getMaxTentativi();
            dto.intervallo = policy.getIntervallo();
        }
        return dto;
    }

    public ConfigurazioneEngine toModel() {
        return new ConfigurazioneEngine(new RetryPolicy(maxTentativi, intervallo));
    }
}
