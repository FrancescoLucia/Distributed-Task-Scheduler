package it.unibas.taskscheduler.modello;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConfigurazioneEngine {

    private RetryPolicy retryPolicy;

    public ConfigurazioneEngine(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }
}
