package it.unibas.taskscheduler.engine.scheduling;

import it.unibas.taskscheduler.modello.Task;

import java.util.ArrayList;
import java.util.Optional;

public class FigliDecrescente implements StrategiaSchedulazione {

    @Override
    public int compare(Task task1, Task task2) {
        int figli1 = Optional.ofNullable(task1.getFigli()).orElse(new ArrayList<>()).size();
        int figli2 = Optional.ofNullable(task2.getFigli()).orElse(new ArrayList<>()).size();
        return Integer.compare(figli2, figli1);
    }
}
