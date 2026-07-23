package it.unibas.taskscheduler.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
public class FileIOCommand implements Command {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Params {
        private String operazione;
        private String path;
        private String contenuto;
    }

    private final Params params;
    private volatile boolean annullato = false;
    private volatile Path temporaneoInCorso;

    public FileIOCommand(Params params) {
        this.params = params;
    }

    public Params getParams() {
        return params;
    }

    @Override
    public void esegui() {
        annullato = false;
        String operazione = params.getOperazione();
        Path path = Path.of(params.getPath());
        log.debug("File {} su {}", operazione, path);
        checkAnnullato();
        try {
            switch (operazione) {
                case "SCRIVI" -> scrivi(path);
                case "APPEND" -> Files.writeString(path, params.getContenuto(),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                case "LEGGI" -> leggi(path);
                default -> throw new IllegalArgumentException("Operazione non supportata: " + operazione);
            }
        } catch (IOException e) {
            throw new RuntimeException("Errore I/O su " + path + ": " + e.getMessage(), e);
        }
    }

    private void scrivi(Path path) throws IOException {
        Path temporaneo = Files.createTempFile(path.toAbsolutePath().getParent(), "task-", ".tmp");
        temporaneoInCorso = temporaneo;
        try {
            Files.writeString(temporaneo, params.getContenuto() != null ? params.getContenuto() : "");
            checkAnnullato();
            Files.move(temporaneo, path, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaneo);
            temporaneoInCorso = null;
        }
    }

    private void leggi(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new RuntimeException("File non trovato: " + path);
        }
        String contenuto = Files.readString(path);
        log.info("Letti {} caratteri da {}", contenuto.length(), path);
    }

    private void checkAnnullato() {
        if (annullato) {
            throw new RuntimeException("Operazione su file annullata: " + params.getPath());
        }
    }

    @Override
    public void annulla() {
        annullato = true;
        Path temporaneo = temporaneoInCorso;
        if (temporaneo != null) {
            try {
                Files.deleteIfExists(temporaneo);
            } catch (IOException e) {
                log.warn("Impossibile eliminare il file temporaneo {}: {}", temporaneo, e.getMessage());
            }
        }
    }
}
