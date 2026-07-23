package it.unibas.taskscheduler.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class HttpRequestCommand implements Command {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Params {
        private String url;
        private String metodo;
        private String corpo;
    }

    private final Params params;
    private volatile CompletableFuture<HttpResponse<String>> richiestaInCorso;

    public HttpRequestCommand(Params params) {
        this.params = params;
    }

    public Params getParams() {
        return params;
    }

    @Override
    public void esegui() {
        String metodo = params.getMetodo() != null ? params.getMetodo().toUpperCase() : "GET";
        log.debug("Richiesta HTTP {} {}", metodo, params.getUrl());

        HttpRequest.BodyPublisher body = params.getCorpo() != null
                ? HttpRequest.BodyPublishers.ofString(params.getCorpo())
                : HttpRequest.BodyPublishers.noBody();
        HttpRequest richiesta = HttpRequest.newBuilder()
                .uri(URI.create(params.getUrl()))
                .timeout(Duration.ofSeconds(10))
                .method(metodo, body)
                .build();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        richiestaInCorso = client.sendAsync(richiesta, HttpResponse.BodyHandlers.ofString());
        try {
            HttpResponse<String> risposta = richiestaInCorso.get();
            if (risposta.statusCode() >= 400) {
                throw new RuntimeException("Richiesta HTTP fallita con stato " + risposta.statusCode() + ": " + params.getUrl());
            }
            log.info("Richiesta HTTP {} {} -> {}", metodo, params.getUrl(), risposta.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Richiesta HTTP interrotta: " + params.getUrl(), e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Errore richiesta HTTP verso " + params.getUrl() + ": " + e.getCause().getMessage(), e.getCause());
        }
    }

    @Override
    public void annulla() {
        if (richiestaInCorso != null) {
            richiestaInCorso.cancel(true);
            log.info("Richiesta HTTP annullata: {}", params.getUrl());
        }
    }
}
