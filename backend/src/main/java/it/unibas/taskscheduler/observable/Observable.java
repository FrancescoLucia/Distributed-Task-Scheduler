package it.unibas.taskscheduler.observable;

public interface Observable {
    void aggiungiObserver(TaskObserver osservatore);
    void rimuoviObserver(TaskObserver osservatore);
    void notificaObserver();
}
