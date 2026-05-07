package it.unibas.taskscheduler.observable;

import it.unibas.taskscheduler.modello.Task;

public interface TaskObserver {
    void aggiorna(Task task);
}
