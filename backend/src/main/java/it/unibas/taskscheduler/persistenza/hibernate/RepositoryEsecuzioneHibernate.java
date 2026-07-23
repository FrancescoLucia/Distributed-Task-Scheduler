package it.unibas.taskscheduler.persistenza.hibernate;

import java.util.Collection;
import java.util.Optional;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import it.unibas.taskscheduler.modello.EStatoWorkflow;
import it.unibas.taskscheduler.modello.EsecuzioneWorkflow;
import it.unibas.taskscheduler.persistenza.IRepositoryEsecuzione;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@IfBuildProperty(name = "dao.strategy", stringValue = "hibernate")
@ApplicationScoped
public class RepositoryEsecuzioneHibernate implements IRepositoryEsecuzione, PanacheRepository<EsecuzioneWorkflow> {

    @Override
    @Transactional
    public void persist(EsecuzioneWorkflow esecuzione) {
        if (esecuzione.getId() == null) {
            PanacheRepository.super.persist(esecuzione);
        }
    }

    @Override
    @Transactional
    public void aggiornaStato(EsecuzioneWorkflow esecuzione) {
        update("stato = ?1, dataFine = ?2 where id = ?3",
                esecuzione.getStato(), esecuzione.getDataFine(), esecuzione.getId());
    }

    @Override
    @Transactional
    public Optional<EsecuzioneWorkflow> findByIdOptional(Long id) {
        return find("""
                        select distinct e
                        from EsecuzioneWorkflow e
                        left join fetch e.tasks t
                        left join fetch t.dipendenze
                        where e.id = ?1
                        """, id)
                .firstResultOptional();
    }

    @Override
    @Transactional
    public Optional<EsecuzioneWorkflow> getEsecuzioneInCorso() {
        return find("""
                        select distinct e
                        from EsecuzioneWorkflow e
                        left join fetch e.tasks t
                        left join fetch t.dipendenze
                        where e.stato in (?1, ?2)
                        order by e.dataInizio asc, e.id asc
                        """, EStatoWorkflow.IN_ESECUZIONE, EStatoWorkflow.IN_PAUSA)
                .firstResultOptional();
    }

    @Override
    @Transactional
    public Collection<EsecuzioneWorkflow> findAll(Long workflowId) {
        if (workflowId == null) {
            return list("""
                            select distinct e
                            from EsecuzioneWorkflow e
                            left join fetch e.tasks
                            order by e.dataInizio desc, e.id desc
                            """);
        }
        return list("""
                        select distinct e
                        from EsecuzioneWorkflow e
                        left join fetch e.tasks
                        where e.workflow.id = ?1
                        order by e.dataInizio desc, e.id desc
                        """, workflowId);
    }
}
