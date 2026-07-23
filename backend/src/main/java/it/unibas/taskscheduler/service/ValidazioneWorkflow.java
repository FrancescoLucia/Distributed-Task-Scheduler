package it.unibas.taskscheduler.service;

import com.fasterxml.jackson.databind.JsonNode;
import it.unibas.taskscheduler.command.CommandMapper;
import it.unibas.taskscheduler.rest.dto.TaskImportDTO;
import it.unibas.taskscheduler.rest.dto.WorkflowImportDTO;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ValidazioneWorkflow {

    private static final Set<String> TIPI = Set.of(CommandMapper.SCRIPT, CommandMapper.MATH, CommandMapper.FILE, CommandMapper.HTTP);
    private static final Set<String> OPERAZIONI_MATH = Set.of("FIBONACCI", "FATTORIALE", "SOMMA_PRIMI");
    private static final Set<String> OPERAZIONI_FILE = Set.of("SCRIVI", "APPEND", "LEGGI");
    private static final Set<String> METODI_HTTP = Set.of("GET", "POST");

    private ValidazioneWorkflow() {
    }

    public static List<String> validate(WorkflowImportDTO workflow) {
        List<String> errori = new ArrayList<>();
        if (workflow == null) {
            errori.add("Corpo del workflow mancante");
            return errori;
        }
        if (isVuoto(workflow.getNome())) {
            errori.add("Il campo 'nome' è obbligatorio");
        }
        List<TaskImportDTO> tasks = workflow.getTasks();
        if (tasks == null || tasks.isEmpty()) {
            errori.add("Il workflow deve contenere almeno un task");
            return errori;
        }

        Set<String> nomi = new HashSet<>();
        Set<String> duplicati = new HashSet<>();
        for (TaskImportDTO task : tasks) {
            if (isVuoto(task.getNome())) {
                errori.add("Ogni task deve avere un 'nome' non vuoto");
                continue;
            }
            if (!nomi.add(task.getNome())) {
                duplicati.add(task.getNome());
            }
        }
        duplicati.forEach(nome -> errori.add("Nome task duplicato: '" + nome + "'"));

        for (TaskImportDTO task : tasks) {
            validaTask(task, nomi, errori);
        }

        rilevaCicli(tasks, nomi, errori);
        return errori;
    }

    private static void validaTask(TaskImportDTO task, Set<String> nomi, List<String> errori) {
        String nome = isVuoto(task.getNome()) ? "?" : task.getNome();
        String tipo = task.getTipo();
        if (tipo == null || !TIPI.contains(tipo)) {
            errori.add("Task '" + nome + "': tipo non valido '" + tipo + "' (ammessi: " + TIPI + ")");
        } else {
            validaParametri(nome, tipo, task.getParametri(), errori);
        }

        if (task.getDipendenze() != null) {
            for (String dip : task.getDipendenze()) {
                if (dip != null && dip.equals(task.getNome())) {
                    errori.add("Task '" + nome + "': non può dipendere da sé stesso");
                } else if (!nomi.contains(dip)) {
                    errori.add("Task '" + nome + "': dipendenza verso task inesistente '" + dip + "'");
                }
            }
        }
    }

    private static void validaParametri(String nome, String tipo, JsonNode parametri, List<String> errori) {
        if (parametri == null || parametri.isNull() || !parametri.isObject()) {
            errori.add("Task '" + nome + "': 'parametri' mancante o non è un oggetto");
            return;
        }
        switch (tipo) {
            case CommandMapper.SCRIPT -> {
                if (isVuoto(testo(parametri, "comando"))) {
                    errori.add("Task '" + nome + "' (SCRIPT): 'comando' obbligatorio");
                }
            }
            case CommandMapper.MATH -> {
                String operazione = testo(parametri, "operazione");
                if (operazione == null || !OPERAZIONI_MATH.contains(operazione)) {
                    errori.add("Task '" + nome + "' (MATH): 'operazione' non valida (ammesse: " + OPERAZIONI_MATH + ")");
                }
                if (!parametri.hasNonNull("n") || !parametri.get("n").canConvertToLong() || parametri.get("n").asLong() < 0) {
                    errori.add("Task '" + nome + "' (MATH): 'n' deve essere un intero ≥ 0");
                }
            }
            case CommandMapper.FILE -> {
                String operazione = testo(parametri, "operazione");
                if (operazione == null || !OPERAZIONI_FILE.contains(operazione)) {
                    errori.add("Task '" + nome + "' (FILE): 'operazione' non valida (ammesse: " + OPERAZIONI_FILE + ")");
                }
                if (isVuoto(testo(parametri, "path"))) {
                    errori.add("Task '" + nome + "' (FILE): 'path' obbligatorio");
                }
            }
            case CommandMapper.HTTP -> {
                String url = testo(parametri, "url");
                if (isVuoto(url) || !uriValido(url)) {
                    errori.add("Task '" + nome + "' (HTTP): 'url' mancante o non valido");
                }
                String metodo = testo(parametri, "metodo");
                if (metodo == null || !METODI_HTTP.contains(metodo.toUpperCase())) {
                    errori.add("Task '" + nome + "' (HTTP): 'metodo' deve essere GET o POST");
                }
            }
            default -> {
            }
        }
    }

    private static void rilevaCicli(List<TaskImportDTO> tasks, Set<String> nomi, List<String> errori) {
        Map<String, List<String>> adiacenza = new HashMap<>();
        for (TaskImportDTO task : tasks) {
            if (isVuoto(task.getNome())) {
                continue;
            }
            List<String> dipendenzeValide = new ArrayList<>();
            if (task.getDipendenze() != null) {
                for (String dip : task.getDipendenze()) {
                    if (nomi.contains(dip)) {
                        dipendenzeValide.add(dip);
                    }
                }
            }
            adiacenza.put(task.getNome(), dipendenzeValide);
        }

        Map<String, Integer> colore = new HashMap<>();
        for (String nodo : adiacenza.keySet()) {
            if (haCiclo(nodo, adiacenza, colore)) {
                errori.add("Il grafo delle dipendenze contiene un ciclo (deve essere un DAG)");
                return;
            }
        }
    }

    private static boolean haCiclo(String nodo, Map<String, List<String>> adiacenza, Map<String, Integer> colore) {
        Integer stato = colore.get(nodo);
        if (stato != null && stato == 1) {
            return true;
        }
        if (stato != null && stato == 2) {
            return false;
        }
        colore.put(nodo, 1);
        for (String vicino : adiacenza.getOrDefault(nodo, List.of())) {
            if (haCiclo(vicino, adiacenza, colore)) {
                return true;
            }
        }
        colore.put(nodo, 2);
        return false;
    }

    private static String testo(JsonNode parametri, String campo) {
        JsonNode valore = parametri.get(campo);
        return valore != null && !valore.isNull() ? valore.asText() : null;
    }

    private static boolean uriValido(String url) {
        try {
            URI.create(url);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isVuoto(String valore) {
        return valore == null || valore.isBlank();
    }
}
