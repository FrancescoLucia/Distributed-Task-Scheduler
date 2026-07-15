package it.unibas.taskscheduler.modello;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Embeddable
public class RetryPolicy {

    @Column(name = "max_tentativi", nullable = false)
    private int maxTentativi;

    @Column(nullable = false)
    private int intervallo;

    public RetryPolicy(int maxTentativi, int intervallo) {
        this.maxTentativi = maxTentativi;
        this.intervallo = intervallo;
    }
}
