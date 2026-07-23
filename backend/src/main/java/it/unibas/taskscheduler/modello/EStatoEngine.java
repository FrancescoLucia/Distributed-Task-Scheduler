package it.unibas.taskscheduler.modello;

public enum EStatoEngine {
    INATTIVO, IN_ESECUZIONE, IN_PAUSA;

    public static EStatoEngine daStatoWorkflow(EStatoWorkflow stato) {
        return switch (stato) {
            case IN_ESECUZIONE -> IN_ESECUZIONE;
            case IN_PAUSA -> IN_PAUSA;
            default -> INATTIVO;
        };
    }
}
