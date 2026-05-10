package it.unibas.taskscheduler.modello;

import it.unibas.taskscheduler.observable.TaskObserver;
import it.unibas.taskscheduler.command.Command;
import it.unibas.taskscheduler.observable.Observable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"dipendenze", "figli", "observer"})
@ToString(exclude = {"dipendenze", "figli", "observer"})
public class Task implements Observable {

    private Long id;
    private Long workflowId;
    private String nome;
    private EStatoTask stato = EStatoTask.IN_ATTESA;
    private Command azione;
    private List<Task> dipendenze = new ArrayList<>();
    private List<Task> figli = new ArrayList<>();
    private int tentativi = 0;

    private transient List<TaskObserver> observer = new ArrayList<>();

    public Task(String nome, Command azione) {
        this.nome = nome;
        this.azione = azione;
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
            (statoAttuale.equals(EStatoTask.FALLITO) && !nuovoStato.equals(EStatoTask.PRONTO)) ||
            (nuovoStato.equals(EStatoTask.IN_ESECUZIONE) && !statoAttuale.equals(EStatoTask.PRONTO))
        );
        return !invalido;
    }

    public void incrementaTentativi() {
        this.tentativi++;
    }
}
