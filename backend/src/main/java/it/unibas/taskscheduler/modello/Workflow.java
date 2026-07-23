package it.unibas.taskscheduler.modello;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "workflows")
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "data_creazione", nullable = false)
    private LocalDateTime dataCreazione = LocalDateTime.now();

    @Column(name = "definizione_json", nullable = false, columnDefinition = "text")
    private String definizioneJson;

    @Column(name = "numero_task", nullable = false)
    private int numeroTask;
}
