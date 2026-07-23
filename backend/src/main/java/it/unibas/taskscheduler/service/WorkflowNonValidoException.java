package it.unibas.taskscheduler.service;

import java.util.List;

public class WorkflowNonValidoException extends RuntimeException {

    private final List<String> errori;

    public WorkflowNonValidoException(List<String> errori) {
        super("Workflow non valido: " + String.join("; ", errori));
        this.errori = errori;
    }

    public List<String> getErrori() {
        return errori;
    }
}
