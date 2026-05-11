package it.unibas.taskscheduler.modello;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import it.unibas.taskscheduler.engine.Engine;
import it.unibas.taskscheduler.observable.TaskObserver;

@Data
@NoArgsConstructor
public class Workflow {

    private Long id;
    private String nome;
    private LocalDateTime dataCreazione = LocalDateTime.now();
    private EStatoWorkflow stato = EStatoWorkflow.IN_PAUSA;
    private Set<Task> tasks = new HashSet<>();

    public void aggiungiTask(Task task) {
        this.tasks.add(task);
    }

    public void trasmettiStatoAiTask(EStatoTask stato) {
        tasks.stream()
                .filter(t -> t.getStato().equals(EStatoTask.IN_ATTESA) || t.getStato().equals(EStatoTask.PRONTO) || t.getStato().equals(EStatoTask.IN_ESECUZIONE))
                .forEach(t -> t.setStato(stato));
    }

    public void pausa() {
        this.stato = EStatoWorkflow.IN_PAUSA;
    }

    public void riprendi() {
       this.stato = EStatoWorkflow.IN_ESECUZIONE;
    }

    public void annulla() {
        this.stato = EStatoWorkflow.ANNULLATO;
    }

    public void inizializzaFigli(TaskObserver taskObserver) {
        for (Task task : this.tasks) {
            task.getDipendenze().forEach(parent -> parent.getFigli().add(task));
            if (task.getDipendenze().isEmpty()) {
                task.setStato(EStatoTask.PRONTO);
            }
            task.aggiungiObserver(taskObserver);
        }
    }

	public void avvia() {
		this.stato = EStatoWorkflow.IN_ESECUZIONE;
	}
}
