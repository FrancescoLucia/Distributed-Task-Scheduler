package it.unibas.taskscheduler.modello;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RetryPolicy {

    private int maxTentativi;
    private int intervallo;

    public RetryPolicy(int maxTentativi, int intervallo) {
        this.maxTentativi = maxTentativi;
        this.intervallo = intervallo;
    }
}
