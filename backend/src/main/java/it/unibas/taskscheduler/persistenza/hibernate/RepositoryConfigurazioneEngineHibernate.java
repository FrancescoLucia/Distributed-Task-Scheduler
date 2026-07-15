package it.unibas.taskscheduler.persistenza.hibernate;

import java.util.Optional;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import it.unibas.taskscheduler.modello.ConfigurazioneEngine;
import it.unibas.taskscheduler.persistenza.IRepositoryConfigurazioneEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@IfBuildProperty(name = "dao.strategy", stringValue = "hibernate")
@ApplicationScoped
public class RepositoryConfigurazioneEngineHibernate
        implements IRepositoryConfigurazioneEngine, PanacheRepository<ConfigurazioneEngine> {

    @Override
    @Transactional
    public void persist(ConfigurazioneEngine configurazione) {
        configurazione.setId(ConfigurazioneEngine.SINGLETON_ID);
        find().ifPresentOrElse(
                configurazioneEsistente -> configurazioneEsistente.setRetryPolicy(configurazione.getRetryPolicy()),
                () -> PanacheRepository.super.persist(configurazione));
    }

    @Override
    @Transactional
    public Optional<ConfigurazioneEngine> find() {
        return findByIdOptional(ConfigurazioneEngine.SINGLETON_ID);
    }
}
