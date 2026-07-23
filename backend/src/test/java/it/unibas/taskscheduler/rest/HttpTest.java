package it.unibas.taskscheduler.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.unibas.taskscheduler.modello.ConfigurazioneEngine;
import it.unibas.taskscheduler.modello.RetryPolicy;
import it.unibas.taskscheduler.persistenza.IRepositoryConfigurazioneEngine;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
class HttpTest {

    @Inject
    IRepositoryConfigurazioneEngine repositoryConfigurazione;

    @TempDir
    Path tempDir;

    @BeforeEach
    void policyRetryVeloce() {
        repositoryConfigurazione.persist(new ConfigurazioneEngine(new RetryPolicy(1, 0)));
    }

    private long importaWorkflow(String json) {
        return given()
                .contentType(ContentType.JSON)
                .body(json)
                .when().post("/workflow/importa")
                .then().statusCode(200)
                .extract().jsonPath().getLong("id");
    }

    private long avviaWorkflow(long workflowId, String algoritmo) {
        return given()
                .when().post("/workflow/" + workflowId + "/avvia?algoritmo=" + algoritmo)
                .then().statusCode(200)
                .extract().jsonPath().getLong("esecuzioneId");
    }

    private String attendiStatoFinale(long esecuzioneId) {
        long scadenza = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < scadenza) {
            String stato = given().when().get("/esecuzioni/" + esecuzioneId)
                    .then().statusCode(200).extract().jsonPath().getString("stato");
            if (stato.equals("COMPLETATO") || stato.equals("FALLITO") || stato.equals("ANNULLATO")) {
                return stato;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Interrotto durante l'attesa dello stato finale");
            }
        }
        fail("Timeout in attesa dello stato finale dell'esecuzione " + esecuzioneId);
        return null;
    }

    @Test
    void pipelineCompletaImportAvvioSchedulazioneECompletamento() {
        Path fileB = tempDir.resolve("b.txt");
        Path markerC = tempDir.resolve("c.done");

        String json = """
                {
                  "nome": "E2E Diamante",
                  "tasks": [
                    { "nome": "A", "tipo": "MATH", "parametri": { "operazione": "FIBONACCI", "n": 10 }, "dipendenze": [] },
                    { "nome": "B", "tipo": "FILE", "parametri": { "operazione": "SCRIVI", "path": "%s", "contenuto": "output" }, "dipendenze": ["A"] },
                    { "nome": "C", "tipo": "SCRIPT", "parametri": { "comando": "sleep 0.2 && touch '%s'" }, "dipendenze": ["A"] },
                    { "nome": "D", "tipo": "SCRIPT", "parametri": { "comando": "test -f '%s' && test -f '%s'" }, "dipendenze": ["B", "C"] }
                  ]
                }
                """.formatted(fileB, markerC, fileB, markerC);

        long workflowId = importaWorkflow(json);
        long esecuzioneId = avviaWorkflow(workflowId, "FIGLI_DESC");

        String statoFinale = attendiStatoFinale(esecuzioneId);
        assertEquals("COMPLETATO", statoFinale);

        given().when().get("/esecuzioni/" + esecuzioneId)
                .then().statusCode(200)
                .body("taskTotali", equalTo(4))
                .body("taskCompletati", equalTo(4));

        given().when().get("/esecuzioni/" + esecuzioneId + "/runtime")
                .then().statusCode(200)
                .body("graph.nodi", hasSize(4))
                .body("graph.nodi.stato", everyItem(equalTo("COMPLETATO")));
    }

    @Test
    void importoDiUnWorkflowNonValidoRestituisce400() {
        String json = """
                { "nome": "", "tasks": [] }
                """;

        given().contentType(ContentType.JSON).body(json)
                .when().post("/workflow/importa")
                .then().statusCode(400)
                .body("errori", hasSize(greaterThan(0)));
    }

    @Test
    void pausaRiprendiEAnnullaAttraversoLeApiRest() {
        String json = """
                {
                  "nome": "E2E Pausa",
                  "tasks": [
                    { "nome": "Lento", "tipo": "SCRIPT", "parametri": { "comando": "sleep 0.6" }, "dipendenze": [] }
                  ]
                }
                """;

        long workflowId = importaWorkflow(json);
        long esecuzioneId = avviaWorkflow(workflowId, "FIGLI_DESC");

        given().when().post("/esecuzioni/" + esecuzioneId + "/pausa")
                .then().statusCode(204);
        given().when().get("/esecuzioni/" + esecuzioneId)
                .then().statusCode(200).body("stato", equalTo("IN_PAUSA"));

        given().when().post("/esecuzioni/" + esecuzioneId + "/pausa")
                .then().statusCode(400);

        given().when().post("/esecuzioni/" + esecuzioneId + "/riprendi")
                .then().statusCode(204);
        given().when().get("/esecuzioni/" + esecuzioneId)
                .then().statusCode(200).body("stato", equalTo("IN_ESECUZIONE"));

        String statoFinale = attendiStatoFinale(esecuzioneId);
        assertEquals("COMPLETATO", statoFinale);
    }

    @Test
    void annullamentoTramiteApiRest() {
        String json = """
                {
                  "nome": "E2E Annulla",
                  "tasks": [
                    { "nome": "Lento", "tipo": "SCRIPT", "parametri": { "comando": "sleep 0.6" }, "dipendenze": [] }
                  ]
                }
                """;

        long workflowId = importaWorkflow(json);
        long esecuzioneId = avviaWorkflow(workflowId, "FIGLI_DESC");

        given().when().post("/esecuzioni/" + esecuzioneId + "/annulla")
                .then().statusCode(204);
        given().when().get("/esecuzioni/" + esecuzioneId)
                .then().statusCode(200).body("stato", equalTo("ANNULLATO"));
    }

    @Test
    void workflowFalliceDopoEsaurimentoTentativiDiRetry() {
        String json = """
                {
                  "nome": "E2E Fallimento",
                  "tasks": [
                    { "nome": "SempreFallito", "tipo": "SCRIPT", "parametri": { "comando": "exit 1" }, "dipendenze": [] }
                  ]
                }
                """;

        long workflowId = importaWorkflow(json);
        long esecuzioneId = avviaWorkflow(workflowId, "FIGLI_DESC");

        String statoFinale = attendiStatoFinale(esecuzioneId);
        assertEquals("FALLITO", statoFinale);

        given().when().get("/esecuzioni/" + esecuzioneId + "/runtime")
                .then().statusCode(200)
                .body("graph.nodi[0].stato", equalTo("FALLITO"))
                .body("graph.nodi[0].tentativi", equalTo(1));
    }
}
