package it.unibas.taskscheduler.modello;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import it.unibas.taskscheduler.observable.TaskObserver;

@Data
@NoArgsConstructor
@Entity
@Table(name = "workflows")
@EqualsAndHashCode(exclude = "tasks")
@ToString(exclude = "tasks")
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "data_creazione", nullable = false)
    private LocalDateTime dataCreazione = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EStatoWorkflow stato = EStatoWorkflow.IN_PAUSA;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Task> tasks = new HashSet<>();

    public void aggiungiTask(Task task) {
        task.setWorkflow(this);
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
        ricostruisciFigli();
        for (Task task : this.tasks) {
            if (task.getDipendenze().isEmpty()) {
                task.setStato(EStatoTask.PRONTO);
            }
            task.aggiungiObserver(taskObserver);
        }
    }

    public void inizializzaRuntime(TaskObserver taskObserver) {
        ricostruisciFigli();
        for (Task task : this.tasks) {
            task.inizializzaAzione();
            task.aggiungiObserver(taskObserver);
        }
    }

    public void ricostruisciFigli() {
        for (Task task : this.tasks) {
            task.getFigli().clear();
            task.setWorkflow(this);
        }
        for (Task task : this.tasks) {
            task.getDipendenze().forEach(parent -> parent.getFigli().add(task));
        }
    }

	public void avvia() {
		this.stato = EStatoWorkflow.IN_ESECUZIONE;
	}
}
