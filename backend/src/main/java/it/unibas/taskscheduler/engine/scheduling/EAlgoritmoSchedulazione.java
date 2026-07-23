package it.unibas.taskscheduler.engine.scheduling;

public enum EAlgoritmoSchedulazione {

    DIPENDENZE_ASC(new DipendenzeCrescente()),
    DIPENDENZE_DESC(new DipendenzeDecrescente()),
    FIGLI_ASC(new FigliCrescente()),
    FIGLI_DESC(new FigliDecrescente()),
    ALFABETICO(new Alfabetico()),
    CASUALE(new Casuale());

    private final StrategiaSchedulazione strategia;

    EAlgoritmoSchedulazione(StrategiaSchedulazione strategia) {
        this.strategia = strategia;
    }

    public StrategiaSchedulazione getStrategia() {
        return strategia;
    }
}
