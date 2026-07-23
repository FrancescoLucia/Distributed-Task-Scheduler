package it.unibas.taskscheduler.modello;

import it.unibas.taskscheduler.command.CommandMapper;
import it.unibas.taskscheduler.observable.TaskObserver;
import it.unibas.taskscheduler.command.Command;
import it.unibas.taskscheduler.observable.Observable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "tasks")
@EqualsAndHashCode(exclude = {"esecuzione", "dipendenze", "figli", "observer", "azione"})
@ToString(exclude = {"esecuzione", "dipendenze", "figli", "observer", "azione"})
public class Task implements Observable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    private Long esecuzioneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "esecuzione_id", nullable = false)
    private EsecuzioneWorkflow esecuzione;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EStatoTask stato = EStatoTask.IN_ATTESA;

    @Transient
    private Command azione;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "task_dependencies",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "dependency_task_id")
    )
    private List<Task> dipendenze = new ArrayList<>();

    @Transient
    private List<Task> figli = new ArrayList<>();

    @Column(nullable = false)
    private int tentativi = 0;

    @Column(name = "command_type", nullable = false)
    private String commandType;

    @Column(name = "command_payload", nullable = false)
    private String commandPayload;

    private transient List<TaskObserver> observer = new ArrayList<>();

    public Task(String nome, Command azione) {
        this.nome = nome;
        setAzione(azione);
    }

    public Long getEsecuzioneId() {
        if (esecuzione != null) {
            return esecuzione.getId();
        }
        return esecuzioneId;
    }

    public void setEsecuzioneId(Long esecuzioneId) {
        this.esecuzioneId = esecuzioneId;
    }

    public void setEsecuzione(EsecuzioneWorkflow esecuzione) {
        this.esecuzione = esecuzione;
        this.esecuzioneId = esecuzione != null ? esecuzione.getId() : null;
    }

    public void setAzione(Command azione) {
        this.azione = azione;
        if (azione != null) {
            this.commandType = CommandMapper.typeOf(azione);
            this.commandPayload = CommandMapper.payloadOf(azione);
        }
    }

    public void setStato(EStatoTask nuovoStato) {
        if (!checkCambioStatoValido(this.stato, nuovoStato)) {
            throw new IllegalArgumentException("Nuovo stato non consentito");
        }
        this.stato = nuovoStato;
        notificaObserver();
    }

    @Override
    public void aggiungiObserver(TaskObserver osservatore) {
        if (observer != null && !observer.contains(osservatore)) {
            observer.add(osservatore);
        }
    }

    @Override
    public void rimuoviObserver(TaskObserver osservatore) {
        if (observer != null) {
            observer.remove(osservatore);
        }
    }

    @Override
    public void notificaObserver() {
        if (observer != null) {
            for (TaskObserver osservatore : observer) {
                osservatore.aggiorna(this);
            }
        }
    }

    private static boolean checkCambioStatoValido(EStatoTask statoAttuale, EStatoTask nuovoStato) {
        boolean invalido = (
            statoAttuale.equals(nuovoStato) ||
            statoAttuale.equals(EStatoTask.COMPLETATO) ||
            statoAttuale.equals(EStatoTask.ANNULLATO) ||
            (statoAttuale.equals(EStatoTask.PRONTO) && nuovoStato.equals(EStatoTask.IN_ATTESA)) ||
            (nuovoStato.equals(EStatoTask.COMPLETATO) && !statoAttuale.equals(EStatoTask.IN_ESECUZIONE)) ||
            (statoAttuale.equals(EStatoTask.FALLITO) && !nuovoStato.equals(EStatoTask.PRONTO) && !nuovoStato.equals(EStatoTask.ANNULLATO)) ||
            (nuovoStato.equals(EStatoTask.IN_ESECUZIONE) && !statoAttuale.equals(EStatoTask.PRONTO))
        );
        return !invalido;
    }

    public void incrementaTentativi() {
        this.tentativi++;
    }

    public void esegui() {
        inizializzaAzione();
        this.azione.esegui();
    }

    public void annulla() {
        inizializzaAzione();
        this.azione.annulla();
    }

    public boolean isCompletato() {
        return this.stato.equals(EStatoTask.COMPLETATO);
    }

    public boolean isInEsecuzione() {
        return this.stato.equals(EStatoTask.IN_ESECUZIONE);
    }

    @PrePersist
    @PreUpdate
    void sincronizzaComandoPersistente() {
        if (azione != null) {
            this.commandType = CommandMapper.typeOf(azione);
            this.commandPayload = CommandMapper.payloadOf(azione);
        }
    }

    @PostLoad
    void inizializzaDopoLoad() {
        this.esecuzioneId = esecuzione != null ? esecuzione.getId() : esecuzioneId;
        this.figli = new ArrayList<>();
        this.observer = new ArrayList<>();
        inizializzaAzione();
    }

    public void inizializzaAzione() {
        if (azione == null && commandType != null && commandPayload != null) {
            this.azione = CommandMapper.from(commandType, commandPayload);
        }
    }
}
