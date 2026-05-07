package it.unibas.taskscheduler.persistenza;

import it.unibas.taskscheduler.modello.ConfigurazioneEngine;
import java.util.Optional;

public interface IRepositoryConfigurazioneEngine {

    void persist(ConfigurazioneEngine configurazione);

    Optional<ConfigurazioneEngine> find();
}
