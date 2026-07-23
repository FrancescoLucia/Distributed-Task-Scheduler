package it.unibas.taskscheduler;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Utilita {

    public static Long generaIdRandomMock(Set<Long> idPresenti) {
        while (true){
            Long id = ThreadLocalRandom.current().nextLong(1, 10000);
            if (!idPresenti.contains(id)) {
                return id;
            }
        }
    }
}
