package it.unibas.taskscheduler.persistenza.mock;

import it.unibas.taskscheduler.modello.ConfigurazioneEngine;
import it.unibas.taskscheduler.persistenza.IRepositoryConfigurazioneEngine;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
@DefaultBean
public class RepositoryConfigurazioneEngineMock implements IRepositoryConfigurazioneEngine {

    private ConfigurazioneEngine configurazione;

    @Override
    public void persist(ConfigurazioneEngine configurazione) {
        this.configurazione = configurazione;
    }

    @Override
    public Optional<ConfigurazioneEngine> find() {
        return Optional.ofNullable(configurazione);
    }
}
