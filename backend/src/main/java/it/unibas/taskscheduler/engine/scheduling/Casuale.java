package it.unibas.taskscheduler.engine.scheduling;

import it.unibas.taskscheduler.modello.Task;

import java.util.concurrent.ThreadLocalRandom;

public class Casuale implements StrategiaSchedulazione {

    @Override
    public int compare(Task task1, Task task2) {
        return ThreadLocalRandom.current().nextInt(-1, 2);
    }
}
