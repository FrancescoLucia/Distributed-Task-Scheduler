package it.unibas.taskscheduler.command;

public interface Command {
    void esegui();
    void annulla(); // Vuoto se il comando non prevede delle azioni di rollback
}
