package it.unibas.taskscheduler.modello;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "engine_config")
public class ConfigurazioneEngine {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Embedded
    private RetryPolicy retryPolicy;

    public ConfigurazioneEngine(RetryPolicy retryPolicy) {
        this.id = SINGLETON_ID;
        this.retryPolicy = retryPolicy;
    }
}
