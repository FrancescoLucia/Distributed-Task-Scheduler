package it.unibas.taskscheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.unibas.taskscheduler.rest.dto.TaskImportDTO;
import it.unibas.taskscheduler.rest.dto.WorkflowImportDTO;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidazioneWorkflowTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private TaskImportDTO task(String nome, String tipo, ObjectNode parametri, String... dipendenze) {
        TaskImportDTO t = new TaskImportDTO();
        t.setNome(nome);
        t.setTipo(tipo);
        t.setParametri(parametri);
        t.setDipendenze(List.of(dipendenze));
        return t;
    }

    private WorkflowImportDTO workflow(String nome, TaskImportDTO... tasks) {
        WorkflowImportDTO w = new WorkflowImportDTO();
        w.setNome(nome);
        w.setTasks(List.of(tasks));
        return w;
    }

    @Test
    void grafoNonValido() {
        ObjectNode math = mapper.createObjectNode().put("operazione", "FIBONACCI").put("n", 10);
        ObjectNode script = mapper.createObjectNode().put("comando", "echo ciao");
        WorkflowImportDTO w = workflow("Diamante",
                task("A", "MATH", math),
                task("B", "SCRIPT", script, "A"),
                task("C", "SCRIPT", script, "A"),
                task("D", "SCRIPT", script, "B", "C"));

        assertTrue(ValidazioneWorkflow.validate(w).isEmpty());
    }


    @Test
    void nomiTaskDuplicati() {
        ObjectNode script = mapper.createObjectNode().put("comando", "echo ciao");
        WorkflowImportDTO w = workflow("Duplicati", task("A", "SCRIPT", script), task("A", "SCRIPT", script));
        assertFalse(ValidazioneWorkflow.validate(w).isEmpty());
    }

    @Test
    void tipoNonValido() {
        WorkflowImportDTO w = workflow("TipoErrato", task("A", "SFTP", mapper.createObjectNode()));
        assertFalse(ValidazioneWorkflow.validate(w).isEmpty());
    }

    @Test
    void taskNonValido() {
        WorkflowImportDTO w = workflow("MathErrato", task("A", "MATH", mapper.createObjectNode().put("n", 10)));
        assertFalse(ValidazioneWorkflow.validate(w).isEmpty());
    }

   
    @Test
    void dipendenzaFantasma() {
        ObjectNode script = mapper.createObjectNode().put("comando", "echo A");
        WorkflowImportDTO w = workflow("DipFantasma", task("A", "SCRIPT", script, "Fantasma"));
        assertFalse(ValidazioneWorkflow.validate(w).isEmpty());
    }

    @Test
    void grafoCiclico() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("workflows/ciclo.json")) {
            WorkflowImportDTO w = mapper.readValue(in, WorkflowImportDTO.class);
            List<String> errori = ValidazioneWorkflow.validate(w);
            assertTrue(errori.stream().anyMatch(e -> e.contains("ciclo")));
        }
    }
}
