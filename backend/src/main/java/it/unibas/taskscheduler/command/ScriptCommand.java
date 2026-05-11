package it.unibas.taskscheduler.command;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ScriptCommand implements Command {

    private final String comando;
    private volatile Process processo;

    public ScriptCommand(String comando) {
        this.comando = comando;
    }

    @Override
    public void esegui() {
        try {
            log.debug("Esecuzione comando: {}", comando);
            processo = new ProcessBuilder(List.of("bash", "-c", comando))
                    .inheritIO()
                    .start();
            int exitCode = processo.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Comando terminato con exit code " + exitCode + ": " + comando);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Esecuzione interrotta: " + comando, e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Errore esecuzione comando: " + comando, e);
        }
    }

    @Override
    public void annulla() {
        if (processo != null && processo.isAlive()) {
            processo.destroy();
            log.info("Comando annullato: {}", comando);
        }
    }

    // TODO: Eventualmente potrei specificare un comando di rollback da eseguire nel metodo annulla
}
