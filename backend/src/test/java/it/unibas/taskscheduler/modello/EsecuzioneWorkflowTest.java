package it.unibas.taskscheduler.modello;

import it.unibas.taskscheduler.command.ScriptCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EsecuzioneWorkflowTest {

    private Task nuovoTask(String nome) {
        return new Task(nome, new ScriptCommand("true"));
    }

    @Test
    void inizializzaFigli() {
        EsecuzioneWorkflow esecuzione = new EsecuzioneWorkflow();
        Task a = nuovoTask("A");
        Task b = nuovoTask("B");
        b.getDipendenze().add(a);
        esecuzione.aggiungiTask(a);
        esecuzione.aggiungiTask(b);

        esecuzione.inizializzaFigli(t -> {
        });

        assertEquals(EStatoTask.PRONTO, a.getStato());
        assertEquals(EStatoTask.IN_ATTESA, b.getStato());
        assertEquals(1, a.getFigli().size());
        assertEquals(b, a.getFigli().get(0));
    }
}
