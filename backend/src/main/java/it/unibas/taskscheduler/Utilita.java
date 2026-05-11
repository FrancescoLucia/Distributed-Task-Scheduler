package it.unibas.taskscheduler;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import it.unibas.taskscheduler.modello.Task;

public class Utilita {

    public static Integer comparaTaskPerNumeroFigli(Task task1, Task task2) {
        int figliTask1 = Optional.ofNullable(task1.getFigli()).orElse(new ArrayList<>()).size();
        int figliTask2 = Optional.ofNullable(task2.getFigli()).orElse(new ArrayList<>()).size();
        return Integer.compare(figliTask2, figliTask1);
    }
    
    public static Long generaIdRandomMock(Set<Long> idPresenti) {
        while (true){
            Long id = ThreadLocalRandom.current().nextLong(1, 10000);
            if (!idPresenti.contains(id)) {
                return id;
            }
        }
    }
}
