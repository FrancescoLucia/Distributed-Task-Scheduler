package it.unibas.taskscheduler.engine.scheduling;

import it.unibas.taskscheduler.modello.Task;

import java.util.ArrayList;
import java.util.Optional;

public class DipendenzeDecrescente implements StrategiaSchedulazione {

    @Override
    public int compare(Task task1, Task task2) {
        int dip1 = Optional.ofNullable(task1.getDipendenze()).orElse(new ArrayList<>()).size();
        int dip2 = Optional.ofNullable(task2.getDipendenze()).orElse(new ArrayList<>()).size();
        return Integer.compare(dip2, dip1);
    }
}
