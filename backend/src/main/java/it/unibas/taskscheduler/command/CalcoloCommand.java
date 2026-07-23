package it.unibas.taskscheduler.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;

@Slf4j
public class CalcoloCommand implements Command {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Params {
        private String operazione;
        private long n;
    }

    private final Params params;
    private volatile boolean annullato = false;

    public CalcoloCommand(Params params) {
        this.params = params;
    }

    public Params getParams() {
        return params;
    }

    @Override
    public void esegui() {
        annullato = false;
        String operazione = params.getOperazione();
        long n = params.getN();
        log.debug("Calcolo {} n={}", operazione, n);
        BigInteger risultato = switch (operazione) {
            case "FIBONACCI" -> fibonacci(n);
            case "FATTORIALE" -> fattoriale(n);
            case "SOMMA_PRIMI" -> sommaPrimi(n);
            default -> throw new IllegalArgumentException("Operazione non supportata: " + operazione);
        };
        log.info("Calcolo {} n={} risultato={}", operazione, n, risultato);
    }

    @Override
    public void annulla() {
        annullato = true;
    }

    private void checkAnnullato() {
        if (annullato) {
            throw new RuntimeException("Calcolo annullato");
        }
    }

    private BigInteger fibonacci(long n) {
        BigInteger a = BigInteger.ZERO;
        BigInteger b = BigInteger.ONE;
        for (long i = 0; i < n; i++) {
            checkAnnullato();
            BigInteger successivo = a.add(b);
            a = b;
            b = successivo;
        }
        return a;
    }

    private BigInteger fattoriale(long n) {
        BigInteger risultato = BigInteger.ONE;
        for (long i = 2; i <= n; i++) {
            checkAnnullato();
            risultato = risultato.multiply(BigInteger.valueOf(i));
        }
        return risultato;
    }

    private BigInteger sommaPrimi(long n) {
        BigInteger somma = BigInteger.ZERO;
        for (long candidato = 2; candidato <= n; candidato++) {
            checkAnnullato();
            if (isPrimo(candidato)) {
                somma = somma.add(BigInteger.valueOf(candidato));
            }
        }
        return somma;
    }

    private boolean isPrimo(long valore) {
        if (valore < 2) {
            return false;
        }
        for (long divisore = 2; divisore * divisore <= valore; divisore++) {
            if (valore % divisore == 0) {
                return false;
            }
        }
        return true;
    }
}
