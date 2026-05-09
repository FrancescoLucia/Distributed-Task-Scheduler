package it.unibas.taskscheduler.persistenza.hibernate;

import java.util.Optional;

import it.unibas.taskscheduler.modello.ConfigurazioneEngine;
import it.unibas.taskscheduler.persistenza.IRepositoryConfigurazioneEngine;

public class RepositoryConfigurazioneEngineHibernate implements IRepositoryConfigurazioneEngine{

    @Override
    public void persist(ConfigurazioneEngine configurazione) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'persist'");
    }

    @Override
    public Optional<ConfigurazioneEngine> find() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'find'");
    }

}
