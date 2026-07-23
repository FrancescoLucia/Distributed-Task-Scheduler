package it.unibas.taskscheduler.engine.scheduling;

import it.unibas.taskscheduler.modello.Task;

import java.util.Comparator;

public class Alfabetico implements StrategiaSchedulazione {

    @Override
    public int compare(Task task1, Task task2) {
        return Comparator.nullsLast(Comparator.<String>naturalOrder())
                .compare(task1.getNome(), task2.getNome());
    }
}
