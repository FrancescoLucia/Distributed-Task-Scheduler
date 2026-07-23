package it.unibas.taskscheduler.engine.scheduling;

import it.unibas.taskscheduler.modello.Task;

import java.util.Comparator;

public interface StrategiaSchedulazione extends Comparator<Task> {
}
