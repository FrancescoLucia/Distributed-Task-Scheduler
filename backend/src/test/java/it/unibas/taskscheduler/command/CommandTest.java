package it.unibas.taskscheduler.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandTest {

    @Test
    void fibonacci() {
        CalcoloCommand comando = new CalcoloCommand(new CalcoloCommand.Params("FIBONACCI", 10));
        assertDoesNotThrow(comando::esegui);
    }

    @Test
    void fattorialeConZero() {
        CalcoloCommand comando = new CalcoloCommand(new CalcoloCommand.Params("FATTORIALE", 0));
        assertDoesNotThrow(comando::esegui);
    }

    @Test
    void sommaPrimi() {
        CalcoloCommand comando = new CalcoloCommand(new CalcoloCommand.Params("SOMMA_PRIMI", 20));
        assertDoesNotThrow(comando::esegui);
    }

    @Test
    void operazioneNonSupportata() {
        CalcoloCommand comando = new CalcoloCommand(new CalcoloCommand.Params("SCONOSCIUTA", 1));
        assertThrows(IllegalArgumentException.class, comando::esegui);
    }

    @Test
    void calcoloAnnullatoDuranteEsecuzioneSiInterrompe() throws InterruptedException {
        CalcoloCommand comando = new CalcoloCommand(new CalcoloCommand.Params("SOMMA_PRIMI", 50_000_000));
        java.util.concurrent.atomic.AtomicReference<Throwable> errore = new java.util.concurrent.atomic.AtomicReference<>();
        Thread esecutore = new Thread(() -> {
            try {
                comando.esegui();
            } catch (Throwable t) {
                errore.set(t);
            }
        });
        esecutore.start();
        Thread.sleep(20);
        comando.annulla();
        esecutore.join(5000);
        assertTrue(errore.get() instanceof RuntimeException);
    }

    @Test
    void scriviEPoiLeggiFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("out.txt");
        FileIOCommand scrivi = new FileIOCommand(new FileIOCommand.Params("SCRIVI", file.toString(), "contenuto-test"));
        scrivi.esegui();
        assertEquals("contenuto-test", Files.readString(file));

        FileIOCommand leggi = new FileIOCommand(new FileIOCommand.Params("LEGGI", file.toString(), null));
        assertDoesNotThrow(leggi::esegui);
    }

    @Test
    void appendConcatenaContenuto(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("append.txt");
        new FileIOCommand(new FileIOCommand.Params("SCRIVI", file.toString(), "uno-")).esegui();
        new FileIOCommand(new FileIOCommand.Params("APPEND", file.toString(), "due")).esegui();
        assertEquals("uno-due", Files.readString(file));
    }

    @Test
    void leggiFileInesistenteLanciaEccezione(@TempDir Path dir) {
        Path file = dir.resolve("assente.txt");
        FileIOCommand leggi = new FileIOCommand(new FileIOCommand.Params("LEGGI", file.toString(), null));
        assertThrows(RuntimeException.class, leggi::esegui);
    }

    @Test
    void scriptConExitZeroTermineConSuccesso() {
        ScriptCommand comando = new ScriptCommand("exit 0");
        assertDoesNotThrow(comando::esegui);
    }

    @Test
    void scriptConExitDiversoDaZeroFallisce() {
        ScriptCommand comando = new ScriptCommand("exit 1");
        RuntimeException e = assertThrows(RuntimeException.class, comando::esegui);
        assertTrue(e.getMessage().contains("exit code 1"));
    }
}
